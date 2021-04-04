package io.libzy.repository

import io.libzy.spotify.api.SpotifyApiDelegator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val spotifyApi: SpotifyApiDelegator
) {

    suspend fun fetchDisplayName() = spotifyApi.getProfileInformation().displayName

    suspend fun getUserId() = spotifyApi.getUserId()
}
