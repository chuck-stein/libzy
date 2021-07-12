package io.libzy.database

// TODO: move this to a package io.libzy.persistence.prefs, alongside io.libzy.persistence.db
// TODO: add a constant for spotify prefs name to persistence package
// TODO: migrate all string resource shared pref keys to here
object SharedPrefKeys {
    const val SPOTIFY_INITIAL_SCAN_IN_PROGRESS = "spotify.scanning"
    const val SPOTIFY_CONNECTED = "spotify.connected"
    const val SPOTIFY_USER_ID = "spotify.user.id"
    const val SPOTIFY_ACCESS_TOKEN = "spotify.auth.token"
    const val SPOTIFY_AUTH_EXPIRATION = "spotify.auth.expiration"
}
