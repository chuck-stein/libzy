package io.libzy.persistence.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    private val prefsFlow = dataStore.data.catch { exception ->
        if (exception is IOException) {
            Timber.e(exception, "Error reading preferences")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    fun <T> getFlowOf(key: Preferences.Key<T>) = prefsFlow
        .map { prefs -> prefs[key] }
        .distinctUntilChanged()

    fun <T> getFlowOf(key: Preferences.Key<T>, default: T) = prefsFlow
        .map { prefs -> prefs[key] ?: default }
        .distinctUntilChanged()

    suspend fun edit(transform: suspend (MutablePreferences) -> Unit) = dataStore.edit(transform)

    suspend fun clear() {
        edit { prefs -> prefs.clear() }
    }

    object Keys {
        val SPOTIFY_CONNECTED = booleanPreferencesKey("spotify.connected")
        val SPOTIFY_USER_ID = stringPreferencesKey("spotify.user.id")
        val SPOTIFY_AUTH_TOKEN = stringPreferencesKey("spotify.auth.token")
        val SPOTIFY_AUTH_EXPIRATION_TIMESTAMP_SECONDS = longPreferencesKey("spotify.auth.expiration.timestamp.seconds")
        val LAST_SYNC_TIMESTAMP_MILLIS = longPreferencesKey("spotify.sync.timestamp.millis")
        val ENABLED_QUERY_PARAMS = stringSetPreferencesKey("query.params.enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding.completed")
    }
}
