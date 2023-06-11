package io.libzy.repository

import io.libzy.persistence.prefs.DataStoreKeys.LAST_SYNC_TIMESTAMP_MILLIS
import io.libzy.persistence.prefs.DataStoreKeys.SPOTIFY_CONNECTED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val prefsStore: PrefsStore,
    coroutineScope: CoroutineScope
) {
    val spotifyConnectedState = prefsStore.getFlowOf(SPOTIFY_CONNECTED, default = false)
        .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = false)

    fun isSpotifyConnected() = spotifyConnectedState.value

    suspend fun setSpotifyConnected(isSpotifyConnected: Boolean) {
        prefsStore.edit { prefs ->
            prefs[SPOTIFY_CONNECTED] = isSpotifyConnected
        }
    }

    val lastSyncTimestampMillis = prefsStore.getFlowOf(LAST_SYNC_TIMESTAMP_MILLIS)

    suspend fun setLastSyncTimestamp(lastSyncTimestampMillis: Long) {
        prefsStore.edit { prefs ->
            prefs[LAST_SYNC_TIMESTAMP_MILLIS] = lastSyncTimestampMillis
        }
    }
}