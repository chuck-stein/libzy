package io.libzy.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import io.libzy.domain.Query
import io.libzy.persistence.prefs.DataStoreKeys.ENABLED_QUERY_PARAMS
import io.libzy.persistence.prefs.DataStoreKeys.LAST_SYNC_TIMESTAMP_MILLIS
import io.libzy.persistence.prefs.DataStoreKeys.SPOTIFY_CONNECTED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    coroutineScope: CoroutineScope
) {

    val prefsFlow = dataStore.data.catch { exception ->
        if (exception is IOException) {
            Timber.e(exception, "Error reading preferences")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    fun <T> prefsFlowOf(key: Preferences.Key<T>) = prefsFlow
        .map { prefs -> prefs[key] }
        .distinctUntilChanged()

    fun <T> prefsFlowOf(key: Preferences.Key<T>, default: T) = prefsFlow
        .map { prefs -> prefs[key] ?: default }
        .distinctUntilChanged()

    suspend fun clearPrefs() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun setSpotifyConnected(isSpotifyConnected: Boolean) {
        dataStore.edit { prefs ->
            prefs[SPOTIFY_CONNECTED] = isSpotifyConnected
        }
    }

    suspend fun setEnabledQueryParams(params: Set<Query.Parameter>) {
        dataStore.edit { prefs ->
            prefs[ENABLED_QUERY_PARAMS] = params.map { it.stringValue }.toSet()
        }
    }

    suspend fun setLastSyncTimestamp(lastSyncTimestampMillis: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_SYNC_TIMESTAMP_MILLIS] = lastSyncTimestampMillis
        }
    }

    // TODO: move to a "SessionRepository" class
    val spotifyConnectedState = prefsFlowOf(SPOTIFY_CONNECTED, default = false)
        .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = false)

    fun isSpotifyConnected() = spotifyConnectedState.value
}
