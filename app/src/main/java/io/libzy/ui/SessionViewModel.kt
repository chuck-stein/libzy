package io.libzy.ui

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
import androidx.work.WorkManager
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.repository.SessionRepository
import io.libzy.repository.UserLibraryRepository
import io.libzy.repository.UserProfileRepository
import io.libzy.spotify.auth.SpotifyAccessToken
import io.libzy.spotify.auth.SpotifyAuthCallback
import io.libzy.spotify.auth.SpotifyAuthClientProxy
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.ui.common.LibzyViewModel
import io.libzy.util.connectedToNetwork
import io.libzy.work.enqueuePeriodicLibrarySync
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SessionViewModel @Inject constructor(
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher,
    private val userProfileRepository: UserProfileRepository,
    private val userLibraryRepository: UserLibraryRepository,
    private val sessionRepository: SessionRepository,
    private val analyticsDispatcher: AnalyticsDispatcher,
    private val workManager: WorkManager,
    private val connectivityManager: ConnectivityManager,
    appContext: Context
) : LibzyViewModel<SessionUiState, SessionUiEvent>(), SpotifyAuthClientProxy {

    override val initialUiState = SessionUiState(
        isSpotifyConnected = sessionRepository.isSpotifyConnected(),
        isOnboardingCompleted = sessionRepository.isOnboardingCompleted(),
        areEnoughAlbumsSaved = userLibraryRepository.areEnoughAlbumsSaved()
    )

    private var refreshSpotifyAuthJob: Job? = null

    private var spotifyAuthCallback: SpotifyAuthCallback? = null

    private val baseSpotifyAuthRequest by lazy {
        AuthorizationRequest.Builder(
            appContext.getString(R.string.spotify_client_id),
            AuthorizationResponse.Type.TOKEN,
            appContext.getString(R.string.spotify_auth_redirect_uri)
        ).setScopes(
            arrayOf(
                "user-library-read", "user-library-modify", "app-remote-control",
                "user-read-recently-played", "user-top-read"
            )
        )
    }

    init {
        spotifyAuthDispatcher.authClientProxy = this
        updateSessionState()
    }

    override fun onCleared() {
        super.onCleared()
        spotifyAuthDispatcher.authClientProxy = null
    }

    private fun updateSessionState() {
        viewModelScope.launch {
            sessionRepository.spotifyConnectedState.collect { spotifyConnected ->
                updateUiState {
                    copy(isSpotifyConnected = spotifyConnected)
                }
            }
        }
        viewModelScope.launch {
            sessionRepository.onboardingCompletedState.collect { onboardingCompleted ->
                updateUiState {
                    copy(isOnboardingCompleted = onboardingCompleted)
                }
            }
        }
        viewModelScope.launch {
            userLibraryRepository.enoughAlbumsSavedFlow.collect { enoughAlbumsSaved ->
                updateUiState {
                    copy(areEnoughAlbumsSaved = enoughAlbumsSaved)
                }
            }
        }
    }

    fun onNewSpotifyAuthAvailable() {
        viewModelScope.launch {
            if (shouldRefreshAuth()) {
                refreshSpotifyAuthJob = launch {
                    updateUiState { copy(loading = true) }
                    spotifyAuthDispatcher.requestAuthorization()
                    updateUiState { copy(loading = false) }
                    workManager.enqueuePeriodicLibrarySync(existingWorkPolicy = CANCEL_AND_REENQUEUE)
                }
            }
        }
    }

    private suspend fun shouldRefreshAuth() = sessionRepository.isSpotifyConnected()
            && sessionRepository.isSpotifyAuthExpired()
            && connectivityManager.connectedToNetwork()
            && refreshSpotifyAuthJob?.isActive != true

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
        Timber.e("Error performing Spotify authorization: $reason")
        spotifyAuthCallback?.onFailure(reason)
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
