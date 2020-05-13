package com.chuckstein.libzy.network.auth

data class SpotifyAccessToken(
    val token: String,
    val expiresIn: Int
)