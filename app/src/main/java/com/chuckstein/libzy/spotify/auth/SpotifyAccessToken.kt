package com.chuckstein.libzy.spotify.auth

data class SpotifyAccessToken(
    val token: String,
    val expiresIn: Int
)