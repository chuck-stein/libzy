package io.libzy.repository

import io.libzy.spotify.api.SpotifyApiDelegator
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val spotifyApi: SpotifyApiDelegator
) {

    suspend fun fetchDisplayName(): String? =
        try {
            spotifyApi.fetchProfileInformation().displayName
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Spotify display name")
            null
        }

    suspend fun fetchUserId(): String? =
        try {
            spotifyApi.fetchUserId()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Spotify user ID")
            null
        }
}
