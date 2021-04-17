package io.libzy.spotify.auth

// TODO: delete after migrating away from adamint
data class LegacySpotifyAccessToken(
    val token: String,
    val expiresIn: Int
)
