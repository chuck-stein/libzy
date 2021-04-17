package io.libzy.spotify.auth

interface SpotifyAuthCallback {

    fun onSuccess(accessToken: LegacySpotifyAccessToken)

    fun onFailure(exception: SpotifyAuthException)

}
