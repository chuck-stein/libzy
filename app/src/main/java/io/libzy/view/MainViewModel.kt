package io.libzy.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.repository.UserProfileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val analyticsDispatcher: AnalyticsDispatcher
) : ViewModel() {

    /**
     * Handle a new Spotify session starting.
     *
     * Does any session-specific initialization necessary,
     * such as setting the user ID for analytics to the Spotify user ID.
     */
    fun onNewSpotifySession() {
        viewModelScope.launch {
            val userId = userProfileRepository.getUserId()
            analyticsDispatcher.setUserId(userId)
            userProfileRepository.fetchDisplayName()?.let {
                analyticsDispatcher.setUserDisplayName(it)
            }
        }
    }
}
