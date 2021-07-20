package io.libzy.spotify.auth

interface SpotifyAuthClientProxy {

    fun initiateSpotifyAuthRequest(callback: SpotifyAuthCallback)

}
