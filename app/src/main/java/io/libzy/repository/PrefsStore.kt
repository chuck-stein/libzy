package io.libzy.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
}
