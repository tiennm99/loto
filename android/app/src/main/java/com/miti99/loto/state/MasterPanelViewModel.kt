package com.miti99.loto.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miti99.loto.audio.VoicePlayer
import com.miti99.loto.settings.SettingsRepository
import com.miti99.loto.settings.SettingsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Host (quản trò) state machine — port of `MasterPanel.svelte`.
 *
 * Auto-call coroutine cancels and restarts whenever (autoRunning,
 * autoCallEnabled, autoCallSpeed) changes — observed from the settings flow.
 */
class MasterPanelViewModel(
    private val settingsRepo: SettingsRepository,
    private val callBus: CallBus,
    private val voicePlayer: VoicePlayer,
    private val storage: GameStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(MasterPanelUiState())
    val state: StateFlow<MasterPanelUiState> = _state.asStateFlow()

    private var autoJob: Job? = null
    @Volatile private var cachedSettings: SettingsState = SettingsState.DEFAULT

    init {
        viewModelScope.launch { loadFromStorage() }
        viewModelScope.launch {
            settingsRepo.flow.collect { s ->
                val prev = cachedSettings
                cachedSettings = s
                // If host disabled auto-call mid-run, force-stop.
                if (!s.autoCallEnabled && _state.value.autoRunning) setAutoRunning(false)
                // If speed changed mid-run, re-arm at the new cadence.
                if (_state.value.autoRunning && prev.autoCallSpeed != s.autoCallSpeed) {
                    restartAutoJob()
                }
            }
        }
    }

    fun newGame() {
        voicePlayer.cancel()
        setAutoRunning(false)
        callBus.reset()
        val deck = DeckState.fresh()
        _state.value = MasterPanelUiState(deck = deck, lastCalled = null, callOrder = emptyMap())
        viewModelScope.launch { storage.saveMasterDeck(deck) }
    }

    fun drawNext() {
        val cur = _state.value
        val deck = cur.deck ?: return
        if (deck.remaining.isEmpty()) return
        val next = deck.remaining.first()
        val newDeck = deck.copy(
            called = deck.called + next,
            remaining = deck.remaining.drop(1),
        )
        val newOrder = cur.callOrder + (next to (newDeck.called.size))
        _state.update { it.copy(deck = newDeck, lastCalled = next, callOrder = newOrder) }
        callBus.broadcast(next)
        if (cachedSettings.voiceEnabledMaster) voicePlayer.playNumber(next)
        viewModelScope.launch { storage.saveMasterDeck(newDeck) }
    }

    fun toggleAuto() {
        val deck = _state.value.deck ?: return
        if (deck.remaining.isEmpty()) return
        setAutoRunning(!_state.value.autoRunning)
    }

    private fun setAutoRunning(running: Boolean) {
        if (_state.value.autoRunning == running) return
        _state.update { it.copy(autoRunning = running) }
        if (running) restartAutoJob() else autoJob?.cancel()
    }

    private fun restartAutoJob() {
        autoJob?.cancel()
        autoJob = viewModelScope.launch {
            while (isActive) {
                val ms = cachedSettings.autoCallSpeed * 1000L
                delay(ms)
                val deck = _state.value.deck ?: break
                if (deck.remaining.isEmpty()) {
                    setAutoRunning(false)
                    break
                }
                drawNext()
            }
        }
    }

    private suspend fun loadFromStorage() {
        val deck = storage.loadMasterDeck() ?: return
        val order = deck.called.withIndex().associate { (i, n) -> n to (i + 1) }
        _state.value = MasterPanelUiState(
            deck = deck,
            lastCalled = deck.called.lastOrNull(),
            callOrder = order,
        )
    }

    override fun onCleared() {
        autoJob?.cancel()
        super.onCleared()
    }
}
