package io.libzy.ui

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.repository.UserProfileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val analyticsDispatcher: AnalyticsDispatcher,
    private val context: Context
) : ViewModel() {

    /**
     * Handle a new Spotify session starting.
     *
     * Does any session-specific initialization necessary,
     * such as setting the user ID for analytics to the Spotify user ID.
     */
    fun onNewSpotifySession() {
        viewModelScope.launch {
            userProfileRepository.fetchUserId()?.let {
                saveUserId(it)
                analyticsDispatcher.setUserId(it)
            }
        }
        viewModelScope.launch {
            userProfileRepository.fetchDisplayName()?.let {
                analyticsDispatcher.setUserDisplayName(it)
            }
        }
    }

    private fun saveUserId(userId: String) {
        val spotifyPrefs = context.getSharedPreferences(
            context.getString(R.string.spotify_prefs_name),
            Context.MODE_PRIVATE
        )
        spotifyPrefs.edit {
            putString(context.getString(R.string.spotify_user_id_key), userId)
        }
    }
}
