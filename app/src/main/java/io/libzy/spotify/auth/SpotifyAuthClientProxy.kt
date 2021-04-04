package io.libzy.spotify.auth

interface SpotifyAuthClientProxy {

    fun initiateAuthRequest(callback: SpotifyAuthCallback)

}
