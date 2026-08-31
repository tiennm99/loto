package com.miti99.loto

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Wraps a real [DataStore] (typically [InMemoryDataStore]) but delays every
 * [data] emission until [gate] completes — the unit-test stand-in for the
 * real async gap between a [DataStore] being constructed and its first
 * emission landing.
 *
 * [InMemoryDataStore] alone resolves `dataStore.data.first()` synchronously,
 * which is why the restore-vs-user-action race (H1) and the settings/master
 * restore ordering race (M4) are invisible to a suite that only uses it:
 * `restore()` never actually suspends past a real coroutine dispatch point.
 * This fake reintroduces that suspension deterministically so tests can
 * interleave a user action (or a settings arrival) with an in-flight
 * restore by controlling exactly when [gate] completes.
 *
 * [updateData] (writes) is *not* gated — production code never waits on a
 * write to observe a race, and gating it would only slow tests down.
 */
class SlowDataStore(
    private val delegate: DataStore<Preferences>,
    private val gate: CompletableDeferred<Unit>,
) : DataStore<Preferences> {

    override val data: Flow<Preferences> = flow {
        gate.await()
        emitAll(delegate.data)
    }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = delegate.updateData(transform)
}
