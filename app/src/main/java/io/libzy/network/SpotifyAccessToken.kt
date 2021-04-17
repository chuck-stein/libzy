package io.libzy.network

data class SpotifyAccessToken(
    private val token: String,
    /** The Unix timestamp (in seconds) at which this access token expires (TODO: make this in millis) */
    private val expiration: Int
)
