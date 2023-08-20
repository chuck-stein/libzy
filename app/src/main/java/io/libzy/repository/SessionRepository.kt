package io.libzy.repository

import io.libzy.persistence.prefs.PrefsStore
import io.libzy.persistence.prefs.PrefsStore.Keys.LAST_SYNC_TIMESTAMP_MILLIS
import io.libzy.persistence.prefs.PrefsStore.Keys.ONBOARDING_COMPLETED
import io.libzy.persistence.prefs.PrefsStore.Keys.SPOTIFY_AUTH_EXPIRATION_TIMESTAMP_SECONDS
import io.libzy.persistence.prefs.PrefsStore.Keys.SPOTIFY_AUTH_TOKEN
import io.libzy.persistence.prefs.PrefsStore.Keys.SPOTIFY_CONNECTED
import io.libzy.spotify.auth.SpotifyAccessToken
import io.libzy.util.currentTimeSeconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
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

    suspend fun getSpotifyAuthToken() = prefsStore.getFlowOf(SPOTIFY_AUTH_TOKEN).first()

    suspend fun isSpotifyAuthExpired(): Boolean {
        val spotifyAuthExpiration = prefsStore.getFlowOf(SPOTIFY_AUTH_EXPIRATION_TIMESTAMP_SECONDS, default = 0).first()
        return currentTimeSeconds() > spotifyAuthExpiration
    }

    suspend fun updateSpotifyAuth(accessToken: SpotifyAccessToken) {
        prefsStore.edit { prefs ->
            prefs[SPOTIFY_AUTH_TOKEN] = accessToken.token
            prefs[SPOTIFY_AUTH_EXPIRATION_TIMESTAMP_SECONDS] = currentTimeSeconds() + accessToken.expiresIn
        }
    }

    val onboardingCompletedState = prefsStore.getFlowOf(ONBOARDING_COMPLETED, default = false)
        .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = false)

    fun isOnboardingCompleted() = onboardingCompletedState.value

    suspend fun setOnboardingCompleted(isOnboardingCompleted: Boolean) {
        prefsStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = isOnboardingCompleted
        }
    }
}
