package io.libzy.repository

import io.libzy.spotify.api.SpotifyApiDelegator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val spotifyApi: SpotifyApiDelegator
) {

    // TODO: after alpha testing, never fetch this field or store it or send it to analytics because it's not actually necessary to track
    // TODO: add this as an Amplitude user property
    suspend fun fetchDisplayName() = spotifyApi.getProfileInformation().displayName

    suspend fun getUserId() = spotifyApi.getUserId()
}
