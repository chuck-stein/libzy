package io.libzy.spotify.auth

interface SpotifyAuthCallback {

    fun onSuccess(accessToken: SpotifyAccessToken)

    fun onFailure(reason: String?)

}