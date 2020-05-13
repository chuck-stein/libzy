package com.chuckstein.libzy.network.auth

interface SpotifyAuthCallback {

    fun onSuccess(accessToken: SpotifyAccessToken)

    fun onFailure(exception: SpotifyAuthException)

}