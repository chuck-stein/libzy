package io.libzy.persistence.prefs

object SharedPrefKeys {
    const val SPOTIFY_INITIAL_SYNC_IN_PROGRESS = "spotify.syncing"
    const val SPOTIFY_USER_ID = "spotify.user.id"
    const val SPOTIFY_AUTH_TOKEN = "spotify.auth.token"
    const val SPOTIFY_AUTH_EXPIRATION_TIMESTAMP = "spotify.auth.expiration.timestamp" // UNIX timestamp in seconds
}
