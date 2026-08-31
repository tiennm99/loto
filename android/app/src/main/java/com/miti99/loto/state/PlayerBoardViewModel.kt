package com.miti99.loto.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miti99.loto.audio.VoicePlayerApi
import com.miti99.loto.audio.playBingo
import com.miti99.loto.audio.playWaiting
import com.miti99.loto.game.CardGenerator
import com.miti99.loto.game.PlayerAutoCross
import com.miti99.loto.game.PlayerCard
import com.miti99.loto.settings.AppMode
import com.miti99.loto.settings.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Immutable UI state of the player board. Derived fields (row completeness,
 * waiting cells, section rings) are recomputed on every publish, mirroring
 * the web's `$derived` blocks in `PlayerBoard.svelte`.
 */
data class PlayerUiState(
    val grid: List<List<Int>>? = null,
    val crossed: List<List<Boolean>> = emptyList(),
    val rowComplete: List<Boolean> = emptyList(),
    /** Cells holding the awaited number of a waiting row (drives the pulse). */
    val waitingCells: Set<PlayerCard.Cell> = emptySet(),
    /** Per 3-row section (Tân Tân card bands): any row inside is waiting. */
    val sectionWaiting: List<Boolean> = listOf(false, false, false),
    /** Transient "Chờ N" chip text, auto-dismissed after 5s. */
    val toast: String? = null,
    val showCongrats: Boolean = false,
    /** 1-based row number shown in the Kinh modal. */
    val congratsRow: Int = 0,
    /** 1 = normal, 2 = confetti celebration. */
    val celebrationTier: Int = 1,
)

/**
 * State and commands for the player board, ported from
 * `web/src/lib/PlayerBoard.svelte`: cell toggling with manual-untick
 * tracking, master auto-cross replay, chờ/kinh detection with voice + toast,
 * and round persistence.
 */
class PlayerBoardViewModel(
    private val repository: GameStateRepository,
    private val masterStore: MasterStore,
    private val settings: StateFlow<Settings>,
    private val voicePlayer: VoicePlayerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private var grid: List<List<Int>>? = null
    private var crossed: List<List<Boolean>> = emptyList()
    private val manualUnticks = mutableSetOf<Int>()
    private val celebratedRows = mutableSetOf<Int>()
    private val notifiedWaitingRows = mutableSetOf<Int>()

    // How many entries of the master's called[] were already replayed.
    // Advances strictly even on no-op passes so a single draw never
    // re-fires. Stays 0 across restore so the full history replays
    // idempotently (already-crossed cells are skipped).
    private var lastHandledIndex = 0

    // Tracks called.length across emissions to detect a master "Ván mới"
    // (length transitions >0 → 0), which force-clears player marks in both
    // mode per locked product decision.
    private var prevCalledLen = 0

    private var toastJob: Job? = null

    init {
        viewModelScope.launch {
            restore()
            // Collect after restore so the replay sees the loaded grid.
            masterStore.state.collect { master -> onMasterState(master) }
        }
    }

    private suspend fun restore() {
        val saved = repository.loadPlayerState() ?: return
        grid = saved.grid
        crossed = saved.crossed
        manualUnticks.clear()
        manualUnticks.addAll(saved.manualUnticks)
        // Seed the row trackers so restoring a finished/waiting row does not
        // re-announce it.
        celebratedRows.clear()
        notifiedWaitingRows.clear()
        for (i in saved.grid.indices) {
            if (PlayerCard.isRowComplete(saved.grid, saved.crossed, i)) celebratedRows.add(i)
            if (PlayerCard.getWaitingNumber(saved.grid, saved.crossed, i) != null) {
                notifiedWaitingRows.add(i)
            }
        }
        publish()
    }

    private fun onMasterState(master: MasterRoundState) {
        val len = master.called.size
        val prev = prevCalledLen
        prevCalledLen = len
        // Master "Ván mới": force-clear player marks in both mode. Keyed on
        // any shrink (not only ==0) so a reset conflated with the new
        // round's first draws is still detected; the fall-through replay
        // then applies those draws to the cleared board.
        if (prev > 0 && len < prev && settings.value.mode == AppMode.BOTH && grid != null) {
            crossed = grid!!.map { row -> row.map { false } }
            manualUnticks.clear()
            lastHandledIndex = 0
            celebratedRows.clear()
            notifiedWaitingRows.clear()
            persist()
            publish()
        }
        val result = PlayerAutoCross.applyMasterCalls(
            grid = grid,
            crossed = crossed,
            called = master.called,
            lastHandledIndex = lastHandledIndex,
            manualUnticks = manualUnticks,
            mode = settings.value.mode,
        )
        lastHandledIndex = result.lastHandledIndex
        if (result.changed) {
            crossed = result.crossed
            persist()
            detectRowEvents()
            publish()
        }
    }

    /** Generate a fresh card (the UI owns the confirmation dialog). */
    fun generate() {
        voicePlayer.cancel()
        val newGrid = CardGenerator.generateGrid()
        var newCrossed = newGrid.map { row -> row.map { false } }
        manualUnticks.clear()
        // Replay the master's called[] onto the fresh grid so the host
        // doesn't restart from zero when regenerating mid-game (locked
        // decision); outside both mode just advance the cursor.
        if (settings.value.mode == AppMode.BOTH) {
            val result = PlayerAutoCross.applyMasterCalls(
                grid = newGrid,
                crossed = newCrossed,
                called = masterStore.state.value.called,
                lastHandledIndex = 0,
                manualUnticks = manualUnticks,
                mode = AppMode.BOTH,
            )
            newCrossed = result.crossed
            lastHandledIndex = result.lastHandledIndex
        } else {
            lastHandledIndex = masterStore.state.value.called.size
        }
        grid = newGrid
        crossed = newCrossed
        celebratedRows.clear()
        notifiedWaitingRows.clear()
        dismissToast()
        persist()
        publish(showCongrats = false)
        // The replay itself can complete a row or land one in chờ — the web
        // gets this re-check for free from its reactive effect.
        detectRowEvents()
    }

    /** Clear all marks (the UI owns the confirmation dialog). */
    fun clearMarks() {
        val g = grid ?: return
        voicePlayer.cancel()
        var cleared = g.map { row -> row.map { false } }
        manualUnticks.clear()
        // In both mode, immediately replay the master's called[] (locked
        // decision: clear → re-cross all currently-called numbers).
        if (settings.value.mode == AppMode.BOTH) {
            val result = PlayerAutoCross.applyMasterCalls(
                grid = g,
                crossed = cleared,
                called = masterStore.state.value.called,
                lastHandledIndex = 0,
                manualUnticks = manualUnticks,
                mode = AppMode.BOTH,
            )
            cleared = result.crossed
            lastHandledIndex = result.lastHandledIndex
        } else {
            lastHandledIndex = masterStore.state.value.called.size
        }
        crossed = cleared
        celebratedRows.clear()
        notifiedWaitingRows.clear()
        dismissToast()
        persist()
        publish(showCongrats = false)
        // See generate(): the clear-replay can immediately re-complete rows.
        detectRowEvents()
    }

    /** Toggle one cell (haptics live in the UI layer). */
    fun onCellClick(row: Int, col: Int) {
        val g = grid ?: return
        val num = g[row][col]
        if (num <= 0) return
        val wasCrossed = crossed.getOrNull(row)?.getOrNull(col) == true
        // Track manual unticks of called numbers so future regen/clear
        // replays skip them; re-ticking removes the suppression. Uncalled
        // numbers are not tracked — auto-cross only acts on called ones.
        if (masterStore.state.value.called.contains(num)) {
            if (wasCrossed) manualUnticks.add(num) else manualUnticks.remove(num)
        }
        crossed = crossed.mapIndexed { ri, r ->
            if (ri == row) r.mapIndexed { ci, v -> if (ci == col) !v else v } else r
        }
        persist()
        detectRowEvents()
        publish()
    }

    fun dismissToast() {
        toastJob?.cancel()
        toastJob = null
        if (_uiState.value.toast != null) {
            _uiState.value = _uiState.value.copy(toast = null)
        }
    }

    fun dismissCongrats() {
        if (_uiState.value.showCongrats) {
            _uiState.value = _uiState.value.copy(showCongrats = false)
        }
    }

    override fun onCleared() {
        voicePlayer.cancel()
    }

    /**
     * Detect newly completed and newly waiting rows, mirroring the web's
     * two-pass $effect: at most one Kinh modal per change, then toast +
     * voice for each newly waiting row.
     */
    private fun detectRowEvents() {
        val g = grid ?: return
        if (crossed.isEmpty()) return
        val s = settings.value
        // The master takes over announcer duties in both mode, so its voice
        // flag also drives Chờ/Kinh. Solo players keep their own flag.
        val announce = s.voiceEnabledPlayer || (s.voiceEnabledMaster && s.mode == AppMode.BOTH)

        // Pass 1: at most one bingo popup per change.
        for (i in g.indices) {
            if (i in celebratedRows || !PlayerCard.isRowComplete(g, crossed, i)) continue
            celebratedRows.add(i)
            notifiedWaitingRows.add(i)
            // Tier 2 confetti: 2nd bingo, OR 1st bingo while another row is
            // one cell away — the old "3+ bingos" threshold rarely fired on
            // a 9-row card, leaving most wins under-celebrated.
            val hasActiveCho = g.indices.any { r ->
                r !in celebratedRows && PlayerCard.getWaitingNumber(g, crossed, r) != null
            }
            val tier = if (celebratedRows.size >= 2 || hasActiveCho) 2 else 1
            _uiState.value = _uiState.value.copy(
                showCongrats = true,
                congratsRow = i + 1,
                celebrationTier = tier,
            )
            if (announce) voicePlayer.playBingo()
            break
        }

        // Pass 2: update waiting state for every non-celebrated row.
        for (i in g.indices) {
            if (i in celebratedRows) continue
            val waitNum = PlayerCard.getWaitingNumber(g, crossed, i)
            if (waitNum != null && i !in notifiedWaitingRows) {
                notifiedWaitingRows.add(i)
                showToast("Chờ $waitNum")
                if (announce) {
                    voicePlayer.playWaiting(
                        waitNum,
                        voiceWaitingNumber = s.voiceWaitingNumber,
                        modeIsBoth = s.mode == AppMode.BOTH,
                    )
                }
            } else if (waitNum == null && i in notifiedWaitingRows) {
                notifiedWaitingRows.remove(i)
            }
        }
    }

    private fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.value = _uiState.value.copy(toast = message)
        toastJob = viewModelScope.launch {
            delay(TOAST_DURATION_MS)
            _uiState.value = _uiState.value.copy(toast = null)
        }
    }

    private fun persist() {
        val g = grid ?: return
        val snapshot = PlayerRoundState(g, crossed, manualUnticks.toSet())
        viewModelScope.launch { repository.savePlayerState(snapshot) }
    }

    private fun publish(showCongrats: Boolean? = null) {
        val g = grid
        if (g == null || crossed.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                grid = g,
                crossed = crossed,
                rowComplete = emptyList(),
                waitingCells = emptySet(),
                sectionWaiting = listOf(false, false, false),
                showCongrats = showCongrats ?: _uiState.value.showCongrats,
            )
            return
        }
        val rowComplete = g.indices.map { PlayerCard.isRowComplete(g, crossed, it) }
        val waitingRows = g.indices.map { r ->
            !rowComplete[r] && PlayerCard.getWaitingNumber(g, crossed, r) != null
        }
        val waitingCells = buildSet {
            for (r in g.indices) {
                if (rowComplete[r]) continue
                val num = PlayerCard.getWaitingNumber(g, crossed, r) ?: continue
                val c = g[r].indexOf(num)
                if (c >= 0) add(PlayerCard.Cell(r, c))
            }
        }
        // Tân Tân physical card: 3 stacked 3-row sections; a section band
        // rings while any of its rows is waiting.
        val sectionWaiting = listOf(0, 3, 6).map { start ->
            (start until start + 3).any { waitingRows.getOrNull(it) == true }
        }
        _uiState.value = _uiState.value.copy(
            grid = g,
            crossed = crossed,
            rowComplete = rowComplete,
            waitingCells = waitingCells,
            sectionWaiting = sectionWaiting,
            showCongrats = showCongrats ?: _uiState.value.showCongrats,
        )
    }

    companion object {
        const val TOAST_DURATION_MS = 5_000L
    }
}
