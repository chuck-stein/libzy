package io.libzy.ui

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.persistence.prefs.SharedPrefKeys
import io.libzy.persistence.prefs.getSharedPrefs
import io.libzy.repository.UserProfileRepository
import io.libzy.spotify.auth.SpotifyAccessToken
import io.libzy.spotify.auth.SpotifyAuthCallback
import io.libzy.spotify.auth.SpotifyAuthClientProxy
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthException
import io.libzy.ui.common.EventsOnlyViewModel
import io.libzy.util.currentTimeSeconds
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SessionViewModel @Inject constructor(
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher,
    private val userProfileRepository: UserProfileRepository,
    private val analyticsDispatcher: AnalyticsDispatcher,
    appContext: Context
) : EventsOnlyViewModel<SessionUiEvent>(), SpotifyAuthClientProxy {

    private val sharedPrefs = appContext.getSharedPrefs()

    private var spotifyAuthCallback: SpotifyAuthCallback? = null

    private val spotifyAuthRequest by lazy {
        AuthorizationRequest.Builder(
            appContext.getString(R.string.spotify_client_id),
            AuthorizationResponse.Type.TOKEN,
            appContext.getString(R.string.spotify_auth_redirect_uri)
        )
            .setScopes(arrayOf("user-library-read", "app-remote-control", "user-read-recently-played", "user-top-read"))
            .build()
    }

    init {
        spotifyAuthDispatcher.authClientProxy = this
    }

    override fun onCleared() {
        super.onCleared()
        spotifyAuthDispatcher.authClientProxy = null
    }

    override fun initiateSpotifyAuthRequest(callback: SpotifyAuthCallback) {
        spotifyAuthCallback = callback
        produceUiEvent(SessionUiEvent.SpotifyAuthRequest(spotifyAuthRequest))
    }

    fun handleSpotifyAuthResponse(response: AuthorizationResponse) {
        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> handleSpotifyAuthSuccess(
                SpotifyAccessToken(response.accessToken, response.expiresIn)
            )
            AuthorizationResponse.Type.ERROR -> handleSpotifyAuthFailure(response.error)
            else -> handleSpotifyAuthFailure("Authorization was prematurely cancelled")
        }
        spotifyAuthCallback = null
    }

    private fun handleSpotifyAuthSuccess(accessToken: SpotifyAccessToken) {
        spotifyAuthCallback?.onSuccess(accessToken)
        saveSpotifyAccessToken(accessToken)
        loadSpotifyProfileInfo()
        analyticsDispatcher.sendAuthorizeSpotifyConnectionEvent()
    }

    private fun handleSpotifyAuthFailure(reason: String) {
        val exception = SpotifyAuthException("Error performing Spotify authorization: $reason")
        spotifyAuthCallback?.onFailure(exception)
        Timber.e(exception)
    }

    private fun saveSpotifyAccessToken(accessToken: SpotifyAccessToken) {
        sharedPrefs.edit {
            putString(SharedPrefKeys.SPOTIFY_AUTH_TOKEN, accessToken.token)
            putInt(SharedPrefKeys.SPOTIFY_AUTH_EXPIRATION, currentTimeSeconds() + accessToken.expiresIn)
        }
    }

    private fun loadSpotifyProfileInfo() {
        viewModelScope.launch {
            userProfileRepository.fetchProfileInfo()?.let { profileInfo ->
                sharedPrefs.edit {
                    putString(SharedPrefKeys.SPOTIFY_USER_ID, profileInfo.id)
                }
                analyticsDispatcher.setUserId(profileInfo.id)
                profileInfo.displayName?.let {
                    analyticsDispatcher.setUserDisplayName(it)
                }
            }
        }
    }

    fun isSpotifyConnected() = sharedPrefs.getBoolean(SharedPrefKeys.SPOTIFY_CONNECTED, false)
}