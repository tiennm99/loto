package com.miti99.loto.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miti99.loto.audio.VoicePlayerApi
import com.miti99.loto.audio.playNumber
import com.miti99.loto.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * State and commands for the master (caller) panel, ported from
 * `web/src/lib/MasterPanel.svelte`. Deck data lives in the shared
 * [MasterStore]; this ViewModel adds the auto-call ticker, voice
 * announcements, and the countdown reset signal.
 */
class MasterPanelViewModel(
    private val masterStore: MasterStore,
    private val settings: StateFlow<Settings>,
    private val voicePlayer: VoicePlayerApi,
) : ViewModel() {

    /** Master round data (called history + remaining deck). */
    val masterState: StateFlow<MasterRoundState> = masterStore.state

    private val _autoRunning = MutableStateFlow(false)
    val autoRunning: StateFlow<Boolean> = _autoRunning

    // Whether the UI is visible. The web's ticker paused for free when the
    // page hid (throttled/paused WebView); a native ViewModel outlives
    // onStop, so the Activity drives this flag. Backgrounding pauses the
    // ticker without flipping autoRunning — it resumes on return.
    private val _foreground = MutableStateFlow(true)

    /**
     * Bumped on each draw — signals the countdown ring to re-baseline
     * (the web's `tickCount`).
     */
    private val _tickKey = MutableStateFlow(0)
    val tickKey: StateFlow<Int> = _tickKey

    init {
        // Auto-call ticker. collectLatest tears down and re-arms the loop
        // whenever autoRunning or the relevant settings change — the native
        // equivalent of the web's single $effect over
        // (autoRunning, autoCallEnabled, autoCallSpeed). Disabling the auto
        // setting mid-run also stops the run.
        viewModelScope.launch {
            combine(_autoRunning, _foreground, settings) { running, foreground, s ->
                Quad(running, foreground, s.autoCallEnabled, s.autoCallSpeed)
            }
                .distinctUntilChanged()
                .collectLatest { (running, foreground, enabled, speed) ->
                    if (!running) return@collectLatest
                    if (!enabled) {
                        _autoRunning.value = false
                        return@collectLatest
                    }
                    if (!foreground) return@collectLatest
                    while (true) {
                        delay(speed * 1000L)
                        if (masterStore.state.value.remaining.isEmpty()) {
                            _autoRunning.value = false
                            break
                        }
                        drawNext()
                    }
                }
        }
    }

    /** Start a fresh round (the UI owns the confirmation dialog). */
    fun newGame() {
        voicePlayer.cancel()
        _autoRunning.value = false
        masterStore.startNewGame()
    }

    /** Draw the next number and announce it when the master voice is on. */
    fun drawNext() {
        val next = masterStore.drawNext() ?: return
        _tickKey.value += 1
        if (settings.value.voiceEnabledMaster) {
            voicePlayer.playNumber(next)
        }
    }

    /** Toggle the auto-call run; no-op when the deck is exhausted. */
    fun toggleAuto() {
        if (masterStore.state.value.remaining.isEmpty()) return
        _autoRunning.value = !_autoRunning.value
    }

    /** Driven by the Activity's onStart/onStop (see the ticker note above). */
    fun setForeground(foreground: Boolean) {
        _foreground.value = foreground
        if (!foreground) voicePlayer.cancel()
    }

    override fun onCleared() {
        voicePlayer.cancel()
    }
}

private data class Quad(
    val running: Boolean,
    val foreground: Boolean,
    val enabled: Boolean,
    val speed: Int,
)
