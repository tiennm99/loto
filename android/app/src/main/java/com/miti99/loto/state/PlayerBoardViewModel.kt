package com.miti99.loto.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miti99.loto.audio.VoicePlayer
import com.miti99.loto.game.GameLogic
import com.miti99.loto.settings.SettingsRepository
import com.miti99.loto.settings.SettingsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Player card state machine — port of `PlayerBoard.svelte`.
 *
 * Bingo / waiting idempotency: `celebratedRows` and `notifiedWaitingRows`
 * are sticky sets; only `generate()` and `clear()` reset them. Bus events
 * are de-duplicated by monotonic `id` to defend against re-emit on settings
 * change (per phase 06 risk register).
 */
class PlayerBoardViewModel(
    private val settingsRepo: SettingsRepository,
    private val callBus: CallBus,
    private val voicePlayer: VoicePlayer,
    private val storage: GameStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerBoardUiState())
    val state: StateFlow<PlayerBoardUiState> = _state.asStateFlow()

    private var persistJob: Job? = null

    init {
        viewModelScope.launch { loadFromStorage() }
        viewModelScope.launch {
            combine(callBus.events, settingsRepo.flow.map { it.mode }, ::Pair)
                .collect { (event, mode) ->
                    if (event != null && mode == SettingsState.Mode.BOTH) consumeBusEvent(event)
                }
        }
    }

    fun generate() {
        voicePlayer.cancel()
        val grid = GameLogic.generateGrid().map { it.toList() }
        val crossed = List(grid.size) { List(grid[0].size) { false } }
        _state.value = PlayerBoardUiState(
            grid = grid,
            crossed = crossed,
            rowComplete = List(grid.size) { false },
        )
        persistDebounced()
    }

    fun clear() {
        val cur = _state.value
        val grid = cur.grid ?: return
        voicePlayer.cancel()
        val cleared = List(grid.size) { List(grid[0].size) { false } }
        _state.update {
            it.copy(
                crossed = cleared,
                rowComplete = List(grid.size) { false },
                celebratedRows = emptySet(),
                notifiedWaitingRows = emptySet(),
                bingoEvent = null,
                waitingToast = null,
            )
        }
        persistDebounced()
    }

    fun toggleCell(row: Int, col: Int) {
        val cur = _state.value
        val grid = cur.grid ?: return
        if (grid[row][col] <= 0) return
        val newCrossed = cur.crossed.mapIndexed { r, line ->
            if (r == row) line.mapIndexed { c, v -> if (c == col) !v else v } else line
        }
        applyCrossedChange(newCrossed)
    }

    fun dismissBingo() = _state.update { it.copy(bingoEvent = null) }

    fun dismissWaitingToast() = _state.update { it.copy(waitingToast = null) }

    // ----- internals -----

    private suspend fun loadFromStorage() {
        val (grid, crossed) = storage.loadPlayer() ?: return
        val gridArr = toIntArray(grid)
        val crossedArr = toBoolArray(crossed)
        val rowComplete = List(grid.size) { GameLogic.isRowComplete(gridArr, crossedArr, it) }
        val celebrated = rowComplete.withIndex().filter { it.value }.map { it.index }.toSet()
        val notifiedWaiting = (grid.indices)
            .filter { GameLogic.getWaitingNumber(gridArr, crossedArr, it) != null }
            .toSet()
        _state.value = PlayerBoardUiState(
            grid = grid,
            crossed = crossed,
            rowComplete = rowComplete,
            celebratedRows = celebrated,
            notifiedWaitingRows = notifiedWaiting,
        )
    }

    /**
     * Set-only auto-tick from MasterPanel. Mirrors `PlayerBoard.svelte:136-151`:
     * find the first uncrossed match and mark it; never unmark.
     */
    private fun consumeBusEvent(event: DrawEvent) {
        val cur = _state.value
        if (event.id <= cur.lastConsumedEventId) return
        val grid = cur.grid ?: return run {
            _state.update { it.copy(lastConsumedEventId = event.id) }
        }
        var hit = false
        val newCrossed = cur.crossed.mapIndexed { r, line ->
            line.mapIndexed { c, v ->
                if (!hit && grid[r][c] == event.num && !v) { hit = true; true } else v
            }
        }
        if (hit) {
            applyCrossedChange(newCrossed, lastConsumedEventId = event.id)
        } else {
            _state.update { it.copy(lastConsumedEventId = event.id) }
        }
    }

    /**
     * Two-pass effect mirror of `PlayerBoard.svelte:91-126`. Pass 1: at most
     * one new bingo per state change. Pass 2: waiting toast for every
     * non-celebrated row that just hit "1 remaining".
     */
    private fun applyCrossedChange(
        newCrossed: List<List<Boolean>>,
        lastConsumedEventId: Long? = null,
    ) {
        val cur = _state.value
        val grid = cur.grid ?: return
        val gridArr = toIntArray(grid)
        val crossedArr = toBoolArray(newCrossed)

        val rowComplete = List(grid.size) { GameLogic.isRowComplete(gridArr, crossedArr, it) }

        var celebrated = cur.celebratedRows
        var notifiedWaiting = cur.notifiedWaitingRows
        var bingoEvent: BingoEvent? = cur.bingoEvent
        var waitingToast: WaitingToast? = cur.waitingToast

        val announce = shouldAnnounce()

        // Pass 1: one new bingo
        run {
            for (i in grid.indices) {
                if (i !in celebrated && rowComplete[i]) {
                    celebrated = celebrated + i
                    notifiedWaiting = notifiedWaiting + i
                    val tier = if (celebrated.size >= 3) 2 else 1
                    bingoEvent = BingoEvent(row1Based = i + 1, tier = tier)
                    if (announce) voicePlayer.playBingo()
                    return@run
                }
            }
        }

        // Pass 2: waiting toast / clear
        for (i in grid.indices) {
            if (i in celebrated) continue
            val waitNum = GameLogic.getWaitingNumber(gridArr, crossedArr, i)
            if (waitNum != null && i !in notifiedWaiting) {
                notifiedWaiting = notifiedWaiting + i
                waitingToast = WaitingToast("Chờ $waitNum", System.currentTimeMillis())
                if (announce) voicePlayer.playWaiting(waitNum, includeNumberSettings())
            } else if (waitNum == null && i in notifiedWaiting && !rowComplete[i]) {
                notifiedWaiting = notifiedWaiting - i
            }
        }

        _state.update {
            it.copy(
                crossed = newCrossed,
                rowComplete = rowComplete,
                celebratedRows = celebrated,
                notifiedWaitingRows = notifiedWaiting,
                bingoEvent = bingoEvent,
                waitingToast = waitingToast,
                lastConsumedEventId = lastConsumedEventId ?: it.lastConsumedEventId,
            )
        }
        persistDebounced()
    }

    private fun shouldAnnounce(): Boolean {
        // Read settings flow synchronously via a non-blocking peek — we have
        // a StateFlow upstream so .replayCache exists. For correctness on
        // first-frame, fall back to DEFAULT.
        val s = currentSettings()
        return s.voiceEnabledPlayer ||
            (s.voiceEnabledMaster && s.mode == SettingsState.Mode.BOTH)
    }

    private fun includeNumberSettings(): Boolean = currentSettings().voiceWaitingNumber

    private fun currentSettings(): SettingsState =
        // settingsRepo.voiceFlow is a StateFlow; we don't have a synchronous
        // SettingsState mirror. The phase-09 SettingsViewModel exposes one;
        // for the VM here, we suspend-read at call sites instead. To keep
        // the surface simple, snapshot via a tiny cache updated on every
        // collected emission.
        cachedSettings ?: SettingsState.DEFAULT

    @Volatile private var cachedSettings: SettingsState? = null

    init {
        viewModelScope.launch {
            settingsRepo.flow.collect { cachedSettings = it }
        }
    }

    private fun persistDebounced() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(300)
            val s = _state.value
            val grid = s.grid ?: return@launch
            storage.savePlayerGrid(grid)
            storage.savePlayerCrossed(s.crossed)
        }
    }

    private fun toIntArray(grid: List<List<Int>>): Array<IntArray> =
        Array(grid.size) { r -> IntArray(grid[r].size) { c -> grid[r][c] } }

    private fun toBoolArray(crossed: List<List<Boolean>>): Array<BooleanArray> =
        Array(crossed.size) { r -> BooleanArray(crossed[r].size) { c -> crossed[r][c] } }
}
