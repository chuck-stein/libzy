package com.chuckstein.libzy.auth

data class SpotifyAccessToken(
    val tokenString: String,
    val expiry: Int
)