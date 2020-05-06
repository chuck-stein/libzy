package com.chuckstein.libzy.auth

interface SpotifyAuthServerProxy {

    suspend fun connectSpotify()

    suspend fun refreshAccessToken(): String

}