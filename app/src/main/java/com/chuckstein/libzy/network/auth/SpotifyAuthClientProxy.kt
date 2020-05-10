package com.chuckstein.libzy.network.auth

interface SpotifyAuthClientProxy {

    fun initiateAuthRequest(callback: SpotifyAuthCallback)

}