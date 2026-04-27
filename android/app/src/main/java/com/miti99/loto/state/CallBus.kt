package com.miti99.loto.state

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-scoped event bus. Master broadcasts each draw; PlayerBoard collects in
 * "both" mode for auto-tick. Mirrors `tiennm99/loto/src/lib/call-bus.svelte.js`.
 *
 * `id` monotonically increases so consumers can de-duplicate when the flow
 * re-fires on unrelated dependency changes (e.g. settings.mode flipping).
 */
class CallBus {
    private val nextId = AtomicLong(1)
    private val _events = MutableStateFlow<DrawEvent?>(null)
    val events: StateFlow<DrawEvent?> = _events

    fun broadcast(num: Int) {
        _events.value = DrawEvent(num, System.currentTimeMillis(), nextId.getAndIncrement())
    }

    fun reset() {
        _events.value = null
    }
}
