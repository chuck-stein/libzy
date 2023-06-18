package io.libzy.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.repository.SessionRepository
import io.libzy.repository.UserProfileRepository
import io.libzy.spotify.auth.SpotifyAccessToken
import io.libzy.spotify.auth.SpotifyAuthCallback
import io.libzy.spotify.auth.SpotifyAuthClientProxy
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthException
import io.libzy.ui.common.LibzyViewModel
import io.libzy.work.LibrarySyncWorker
import io.libzy.work.LibrarySyncWorker.Companion.LIBRARY_SYNC_INTERVAL
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SessionViewModel @Inject constructor(
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher,
    private val userProfileRepository: UserProfileRepository,
    private val sessionRepository: SessionRepository,
    private val analyticsDispatcher: AnalyticsDispatcher,
    private val workManager: WorkManager,
    appContext: Context
) : LibzyViewModel<SessionUiState, SessionUiEvent>(), SpotifyAuthClientProxy {

    override val initialUiState = SessionUiState(sessionRepository.isSpotifyConnected())

    private var refreshSpotifyAuthJob: Job? = null

    private var spotifyAuthCallback: SpotifyAuthCallback? = null

    private val baseSpotifyAuthRequest by lazy {
        AuthorizationRequest.Builder(
            appContext.getString(R.string.spotify_client_id),
            AuthorizationResponse.Type.TOKEN,
            appContext.getString(R.string.spotify_auth_redirect_uri)
        ).setScopes(arrayOf("user-library-read", "app-remote-control", "user-read-recently-played", "user-top-read"))
    }

    init {
        spotifyAuthDispatcher.authClientProxy = this

        viewModelScope.launch {
            updateSessionState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        spotifyAuthDispatcher.authClientProxy = null
    }

    private suspend fun updateSessionState() {
        sessionRepository.spotifyConnectedState.collect { spotifyConnected ->
            updateUiState {
                copy(isSpotifyConnected = spotifyConnected)
            }
        }
    }

    fun onNewSpotifyAuthAvailable() {
        viewModelScope.launch {
            val shouldRefreshAuth = sessionRepository.isSpotifyConnected()
                    && sessionRepository.isSpotifyAuthExpired()
                    && refreshSpotifyAuthJob?.isActive != true
            if (shouldRefreshAuth) {
                refreshSpotifyAuthJob = launch {
                    spotifyAuthDispatcher.requestAuthorization()
                    val workRequest = PeriodicWorkRequestBuilder<LibrarySyncWorker>(LIBRARY_SYNC_INTERVAL).build()
                    workManager.enqueueUniquePeriodicWork(
                        LibrarySyncWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                        workRequest
                    )
                }
            }
        }
    }

    override fun initiateSpotifyAuthRequest(
        callback: SpotifyAuthCallback,
        authOptions: AuthorizationRequest.Builder.() -> AuthorizationRequest.Builder
    ) {
        spotifyAuthCallback = callback
        val spotifyAuthRequest = baseSpotifyAuthRequest.authOptions().build()
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
        viewModelScope.launch {
            sessionRepository.updateSpotifyAuth(accessToken)
            loadSpotifyProfileInfo()
            analyticsDispatcher.sendAuthorizeSpotifyConnectionEvent()
        }
    }

    private fun handleSpotifyAuthFailure(reason: String) {
        val exception = SpotifyAuthException("Error performing Spotify authorization: $reason")
        spotifyAuthCallback?.onFailure(exception)
        Timber.e(exception)
    }

    private fun loadSpotifyProfileInfo() {
        viewModelScope.launch {
            userProfileRepository.fetchProfileInfo()?.let { profileInfo ->
                sessionRepository.setSpotifyUserId(profileInfo.id)
                profileInfo.displayName?.let {
                    analyticsDispatcher.setUserDisplayName(it)
                }
            }
        }
    }
}
