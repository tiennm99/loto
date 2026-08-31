package com.miti99.loto

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory [DataStore] for JVM unit tests. File-backed DataStore cannot be
 * exercised on a Windows JVM (its atomic rename fails when the target file
 * exists — java.io.File.renameTo semantics), and repositories only need the
 * [DataStore] contract anyway: "process death" is simulated by pointing a
 * fresh repository at the same store instance.
 */
class InMemoryDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {

    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = mutex.withLock {
        val next = transform(state.value)
        state.value = next
        next
    }
}
