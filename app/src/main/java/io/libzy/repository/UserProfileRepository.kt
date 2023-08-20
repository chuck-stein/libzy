package io.libzy.repository

import io.libzy.persistence.prefs.PrefsStore
import io.libzy.persistence.prefs.PrefsStore.Keys.SPOTIFY_USER_ID
import io.libzy.spotify.api.SpotifyApiDelegator
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UserProfileRepository @Inject constructor(
    private val spotifyApi: SpotifyApiDelegator,
    private val prefsStore: PrefsStore
) {
    suspend fun updateProfileInfo() {
        val profileInfo = spotifyApi.apiCall("fetch Spotify profile info") {
            users.getClientProfile()
        }
        profileInfo?.id?.let {
            prefsStore.edit { prefs ->
                prefs[SPOTIFY_USER_ID] = it
            }
        }
    }

    val spotifyUserId = prefsStore.getFlowOf(SPOTIFY_USER_ID)

    suspend fun getSpotifyUserId() = spotifyUserId.first()
}
