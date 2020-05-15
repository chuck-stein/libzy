package com.chuckstein.libzy.spotify.auth

interface SpotifyAuthClientProxy {

    fun initiateAuthRequest(callback: SpotifyAuthCallback)

}