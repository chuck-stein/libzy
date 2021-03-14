package io.libzy.spotify.auth

// TODO: rename this and its associated instance variables to "SpotifyAuthServerProxy"
interface SpotifyAuthClientProxy {

    fun initiateAuthRequest(callback: SpotifyAuthCallback)

}
