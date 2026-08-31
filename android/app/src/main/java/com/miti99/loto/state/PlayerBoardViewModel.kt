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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    /** Transient "Chờ N" chip number, auto-dismissed after 5s; null = hidden. */
    val waitingNumber: Int? = null,
    val showCongrats: Boolean = false,
    /** 1-based row number shown in the Kinh modal. */
    val congratsRow: Int = 0,
    /** 1 = normal, 2 = confetti celebration. */
    val celebrationTier: Int = 1,
    /**
     * True until [PlayerBoardViewModel]'s startup restore AND the settings
     * DataStore's first read have both resolved (M3). `grid == null` alone
     * cannot distinguish "no card yet" from "not loaded yet" — the UI must
     * gate the no-confirm generate branch on this instead, or a tap during
     * the DataStore read races the restore (see restore()). Folding the
     * settings gate in here too closes a second race: without it, a tap in
     * this window with a persisted `mode = BOTH` and a restored master
     * history would read the settings placeholder default and permanently
     * lose the replay (`generate()`/`clearMarks()` compute their auto-cross
     * cursor eagerly, and the master's called[] does not re-emit on its
     * own once that cursor is wrong).
     */
    val loading: Boolean = true,
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
    /**
     * Null = the settings DataStore's first real read has not resolved yet
     * (M2/M4 residual). A single flow, rather than a separate `settings`
     * snapshot plus a separate `loaded` boolean, is deliberate: two
     * independent collectors of the same upstream settings flow (as
     * `LotoApplication` used to expose) cannot be trusted to update in step
     * with each other, so reading "is it loaded" and "what is the mode" from
     * two different flows could observe two different snapshots — the gate
     * could open while the mode read still returned the placeholder default
     * (M2). Reading both off this one flow's `.value` makes that
     * impossible: they are, by construction, always the same snapshot.
     * Production wiring ([com.miti99.loto.state.LotoViewModelFactory])
     * passes `LotoApplication.settingsOrNull`.
     */
    private val settingsOrNull: StateFlow<Settings?>,
    private val voicePlayer: VoicePlayerApi,
    /**
     * Where [CardGenerator.generateGrid] runs (M5's up-to-200-attempt
     * rejection loop). Overridable so unit tests can substitute the
     * `StandardTestDispatcher` also installed as Main, keeping the whole
     * flow on one deterministic virtual clock instead of racing a real
     * background thread against `runCurrent()`/`advanceUntilIdle()`.
     */
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /**
     * Used only for the narrow pre-load window where [settingsOrNull] is
     * still null but a restored, already-clickable grid can be interacted
     * with (`onCellClick` → `detectRowEvents`). Production wiring supplies
     * the manifest-derived defaults; tests that never exercise that window
     * (i.e. every test whose `settingsOrNull` is a non-null-typed
     * `StateFlow<Settings>`) never read this.
     */
    private val fallbackSettings: Settings = Settings(voice = ""),
) : ViewModel() {

    /** Current settings snapshot, falling back only in the pre-load window (see [fallbackSettings]). */
    private val settings: Settings get() = settingsOrNull.value ?: fallbackSettings

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

    // True once restore()'s DataStore read has resolved. Combined with
    // `settingsOrNull != null` to compute the published `loading` flag (M3)
    // — see publishLoading().
    private var restoreDone = false

    // Guards generate() against a second rapid tap enqueueing a second
    // generation while the first is still computing off-main (L-a): `grid`
    // is only assigned at the very end of applyGeneratedGrid, so a
    // grid == null check alone does not catch an in-flight generation.
    private var generating = false

    init {
        viewModelScope.launch {
            restore()
            // combine (not collect) so a settings arrival that lands after
            // the first master emission re-runs the replay against the
            // current snapshot, and so a settings change also republishes
            // `loading` (M3). masterStore.restore() and the settings
            // DataStore read are two independent async reads that race
            // independently (M4): without this, a master emission evaluated
            // while settings still held its placeholder default would
            // permanently miss a persisted `mode = BOTH`, since
            // masterStore.state does not emit again until the next draw.
            //
            // Gating on `settingsOrNull != null` closes the reverse
            // ordering combine() alone does not: if masterStore's restore
            // lands with the full called history *before* settings ever
            // resolves, evaluating that emission under a placeholder mode
            // would advance lastHandledIndex to called.size with
            // changed = false — a silent no-op that permanently loses the
            // replay, since masterStore.state won't re-emit until the next
            // draw. Skipping onMasterState entirely while not loaded leaves
            // the cursor untouched, so once settings resolves, combine's
            // cached master value is re-delivered and replayed under the
            // real mode. generate()/clearMarks() have their own guard for
            // the same reason (M3): this combine alone does not protect a
            // tap that runs before the first re-evaluation.
            combine(masterStore.state, settingsOrNull) { master, snapshot -> master to snapshot }
                .collect { (master, snapshot) ->
                    publishLoading()
                    if (snapshot != null) onMasterState(master)
                }
        }
    }

    private fun publishLoading() {
        publish(loading = !restoreDone || settingsOrNull.value == null)
    }

    private suspend fun restore() {
        val saved = repository.loadPlayerState()
        // H1: generate()/clearMarks() can run while this suspends on the
        // DataStore read (the UI gates on `loading`, but guard here too in
        // case a caller acts before that gate resolves). Never let a stale
        // persisted round overwrite a card the user already produced, and
        // never overwrite one that is still being computed (L-a).
        if (saved != null && grid == null && !generating) {
            grid = saved.grid
            crossed = saved.crossed
            manualUnticks.clear()
            manualUnticks.addAll(saved.manualUnticks)
            // Seed the row trackers so restoring a finished/waiting row does
            // not re-announce it.
            celebratedRows.clear()
            notifiedWaitingRows.clear()
            for (i in saved.grid.indices) {
                if (PlayerCard.isRowComplete(saved.grid, saved.crossed, i)) celebratedRows.add(i)
                if (PlayerCard.getWaitingNumber(saved.grid, saved.crossed, i) != null) {
                    notifiedWaitingRows.add(i)
                }
            }
        }
        restoreDone = true
        publishLoading()
    }

    private fun onMasterState(master: MasterRoundState) {
        val len = master.called.size
        val prev = prevCalledLen
        prevCalledLen = len
        // Master "Ván mới": force-clear player marks in both mode. Keyed on
        // any shrink (not only ==0) so a reset conflated with the new
        // round's first draws is still detected; the fall-through replay
        // then applies those draws to the cleared board.
        val shrunk = prev > 0 && len < prev
        if (shrunk) {
            // L6: manual-untick suppressions from the previous round must
            // not survive a "Ván mới" in ANY mode, not only BOTH — a manual
            // untick can be recorded whenever a number is in the master's
            // called[] regardless of the currently displayed mode
            // (onCellClick has no mode gate), so a later switch to BOTH +
            // "Xoá đánh dấu" would otherwise replay with suppressions left
            // over from a round that no longer exists.
            val hadUnticks = manualUnticks.isNotEmpty()
            manualUnticks.clear()
            if (settings.mode == AppMode.BOTH && grid != null) {
                crossed = grid!!.map { row -> row.map { false } }
                lastHandledIndex = 0
                celebratedRows.clear()
                notifiedWaitingRows.clear()
                persist()
                publish()
            } else if (hadUnticks) {
                persist()
            }
        }
        val result = PlayerAutoCross.applyMasterCalls(
            grid = grid,
            crossed = crossed,
            called = master.called,
            lastHandledIndex = lastHandledIndex,
            manualUnticks = manualUnticks,
            mode = settings.mode,
        )
        lastHandledIndex = result.lastHandledIndex
        if (result.changed) {
            crossed = result.crossed
            persist()
            detectRowEvents()
            publish()
        }
    }

    /**
     * Generate a fresh card (the UI owns the confirmation dialog).
     * `CardGenerator.generateGrid()` can retry up to 200 times (M5), so the
     * actual generation runs off the main thread; only the pure grid
     * computation is offloaded — the state mutations below still happen on
     * the main dispatcher.
     *
     * No-ops while settings have not resolved yet (M3): the mode-dependent
     * replay below would otherwise compute the auto-cross cursor against a
     * placeholder mode, and — since it is not re-evaluated once settings do
     * resolve — permanently lose the replay. The UI already disables the
     * button on `state.loading` for the same reason; this is the
     * in-case-a-caller-acts-before-that-gate-resolves guard, mirroring
     * restore()'s own defense. Also no-ops while a previous call is still
     * computing (L-a), since `grid` alone cannot detect an in-flight
     * generation.
     */
    fun generate() {
        if (settingsOrNull.value == null || generating) return
        generating = true
        voicePlayer.cancel()
        viewModelScope.launch(computeDispatcher) {
            val newGrid = CardGenerator.generateGrid()
            withContext(Dispatchers.Main.immediate) {
                applyGeneratedGrid(newGrid)
                generating = false
            }
        }
    }

    private fun applyGeneratedGrid(newGrid: List<List<Int>>) {
        var newCrossed = newGrid.map { row -> row.map { false } }
        manualUnticks.clear()
        // Replay the master's called[] onto the fresh grid so the host
        // doesn't restart from zero when regenerating mid-game (locked
        // decision); outside both mode just advance the cursor.
        if (settings.mode == AppMode.BOTH) {
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

    /**
     * Clear all marks (the UI owns the confirmation dialog). No-ops while
     * settings have not resolved yet — same M3 rationale as [generate].
     */
    fun clearMarks() {
        if (settingsOrNull.value == null) return
        val g = grid ?: return
        voicePlayer.cancel()
        var cleared = g.map { row -> row.map { false } }
        manualUnticks.clear()
        // In both mode, immediately replay the master's called[] (locked
        // decision: clear → re-cross all currently-called numbers).
        if (settings.mode == AppMode.BOTH) {
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
        if (_uiState.value.waitingNumber != null) {
            _uiState.value = _uiState.value.copy(waitingNumber = null)
        }
    }

    fun dismissCongrats() {
        if (_uiState.value.showCongrats) {
            _uiState.value = _uiState.value.copy(showCongrats = false)
        }
    }

    override fun onCleared() {
        // L2: see MasterPanelViewModel.onCleared() — voicePlayer is a
        // shared singleton; MainActivity.onDestroy() already stops it on
        // the same isFinishing condition this onCleared() fires under, so
        // cancelling it here too was redundant and coupled two
        // independently-clearable owners to one shared resource.
        super.onCleared()
    }

    /**
     * Detect newly completed and newly waiting rows, mirroring the web's
     * two-pass $effect: at most one Kinh modal per change, then toast +
     * voice for each newly waiting row.
     */
    private fun detectRowEvents() {
        val g = grid ?: return
        if (crossed.isEmpty()) return
        val s = settings
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
                showWaitingToast(waitNum)
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

    private fun showWaitingToast(waitNum: Int) {
        toastJob?.cancel()
        _uiState.value = _uiState.value.copy(waitingNumber = waitNum)
        toastJob = viewModelScope.launch {
            delay(TOAST_DURATION_MS)
            _uiState.value = _uiState.value.copy(waitingNumber = null)
        }
    }

    private fun persist() {
        val g = grid ?: return
        val snapshot = PlayerRoundState(g, crossed, manualUnticks.toSet())
        viewModelScope.launch { repository.savePlayerState(snapshot) }
    }

    private fun publish(showCongrats: Boolean? = null, loading: Boolean? = null) {
        val g = grid
        if (g == null || crossed.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                grid = g,
                crossed = crossed,
                rowComplete = emptyList(),
                waitingCells = emptySet(),
                sectionWaiting = listOf(false, false, false),
                showCongrats = showCongrats ?: _uiState.value.showCongrats,
                loading = loading ?: _uiState.value.loading,
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
            loading = loading ?: _uiState.value.loading,
        )
    }

    companion object {
        const val TOAST_DURATION_MS = 5_000L
    }
}
