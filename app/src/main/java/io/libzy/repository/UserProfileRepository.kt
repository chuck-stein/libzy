package io.libzy.repository

import com.adamratzman.spotify.models.SpotifyUserInformation
import io.libzy.spotify.api.SpotifyApiDelegator
import timber.log.Timber
import javax.inject.Inject

class UserProfileRepository @Inject constructor(
    private val spotifyApi: SpotifyApiDelegator
) {
    suspend fun fetchProfileInfo(): SpotifyUserInformation? =
        try {
            spotifyApi.fetchProfileInfo()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Spotify profile info")
            null
        }
}
