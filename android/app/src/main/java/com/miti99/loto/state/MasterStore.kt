package com.miti99.loto.state

import com.miti99.loto.game.DrawDeck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Shared reactive state for the master draw deck — the native counterpart of
 * `web/src/lib/master-store.svelte.js`. Application-scoped so the player side
 * reads the full `called` history directly (no single-slot bus that loses
 * history). Persists through [GameStateRepository] on every mutation.
 */
class MasterStore(
    private val repository: GameStateRepository,
    private val scope: CoroutineScope,
    private val deck: DrawDeck = DrawDeck(),
) {

    private val _state = MutableStateFlow(MasterRoundState(emptyList(), emptyList()))
    val state: StateFlow<MasterRoundState> = _state

    private val _loading = MutableStateFlow(true)
    /** True until [restore] has resolved once at startup; drives button gating. */
    val loading: StateFlow<Boolean> = _loading

    // Set by startNewGame()/drawNext() so a still-in-flight restore() never
    // clobbers a round the user already started while the DataStore read was
    // pending (H1: restore races "Ván mới" the same way it races the
    // player's "Tạo bảng mới").
    private var mutatedBeforeRestore = false

    /** Restore the persisted round, if any. Call once on startup. */
    suspend fun restore() {
        val saved = repository.loadMasterState()
        if (saved != null && !mutatedBeforeRestore) {
            deck.restore(saved.called, saved.remaining)
            _state.value = saved
        }
        _loading.value = false
    }

    /** Start a fresh round: empty called, full shuffled remaining. */
    fun startNewGame() {
        mutatedBeforeRestore = true
        deck.startNewGame()
        publishAndSave()
    }

    /**
     * Draw the next number from remaining into called.
     * @return the drawn number, or null if exhausted.
     */
    fun drawNext(): Int? {
        val next = deck.drawNext() ?: return null
        mutatedBeforeRestore = true
        publishAndSave()
        return next
    }

    private fun publishAndSave() {
        val snapshot = MasterRoundState(deck.called, deck.remaining)
        _state.value = snapshot
        scope.launch { repository.saveMasterState(snapshot) }
    }
}
