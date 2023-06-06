package io.libzy.persistence.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object DataStoreKeys {
    val SPOTIFY_CONNECTED = booleanPreferencesKey("spotify.connected")
    val ENABLED_QUERY_PARAMS = stringSetPreferencesKey("query.params.enabled")
    val LAST_SYNC_TIMESTAMP_MILLIS = longPreferencesKey("spotify.sync.timestamp.millis")
    // TODO: migrate remaining shared prefs keys to DataStore
}