package com.chuckstein.libzy.network.auth

interface SpotifyAuthCallback {

    fun onSuccess(accessToken: String)

    fun onFailure(exception: SpotifyAuthException)

}