package com.miti99.loto

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * [DataStore] fake that fails every read and write with [exception] — the
 * unit-test stand-in for a full disk, a revoked storage permission, or a
 * corrupted-beyond-recovery backing file (file-backed `DataStore` cannot be
 * exercised on a Windows JVM at all, per [InMemoryDataStore]'s doc).
 * Exercises the L4 fallback paths in
 * `com.miti99.loto.state.GameStateRepository` and
 * `com.miti99.loto.settings.SettingsRepository`: both must degrade to
 * defaults/empty state instead of propagating, and now also log the
 * failure via `android.util.Log.w`.
 *
 * Defaults to [IOException] (the routine full-disk/permission case); pass a
 * non-[IOException] (e.g. `RuntimeException`) to exercise the M4 fail-open
 * path, where a non-IO read failure must still resolve to defaults instead
 * of leaving the settings flow's collectors permanently cancelled.
 */
class ThrowingDataStore(
    private val exception: Throwable = IOException("simulated read failure"),
) : DataStore<Preferences> {

    override val data: Flow<Preferences> = flow { throw exception }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = throw exception
}
