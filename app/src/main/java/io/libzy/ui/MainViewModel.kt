package io.libzy.ui

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.persistence.prefs.SharedPrefKeys
import io.libzy.persistence.prefs.getSharedPrefs
import io.libzy.repository.UserProfileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val analyticsDispatcher: AnalyticsDispatcher,
    context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPrefs()

    /**
     * Handle a new Spotify session starting.
     *
     * Does any session-specific initialization necessary,
     * such as setting the user ID for analytics to the Spotify user ID.
     */
    fun onNewSpotifySession() {
        viewModelScope.launch {
            val userId = userProfileRepository.getUserId()
            saveUserId(userId)
            analyticsDispatcher.setUserId(userId)

            userProfileRepository.fetchDisplayName()?.let {
                analyticsDispatcher.setUserDisplayName(it)
            }
        }
    }

    private fun saveUserId(userId: String) {
        sharedPrefs.edit {
            putString(SharedPrefKeys.SPOTIFY_USER_ID, userId)
        }
    }
}
