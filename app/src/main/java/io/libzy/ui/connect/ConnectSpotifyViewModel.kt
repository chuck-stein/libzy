package io.libzy.ui.connect

import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo.State.CANCELLED
import androidx.work.WorkInfo.State.FAILED
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.repository.SessionRepository
import io.libzy.repository.UserProfileRepository
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthResult
import io.libzy.ui.common.LibzyViewModel
import io.libzy.ui.connect.ConnectSpotifyUiEvent.SPOTIFY_SYNC_FAILED
import io.libzy.util.flatten
import io.libzy.work.LibrarySyncWorker
import io.libzy.work.enqueuePeriodicLibrarySync
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConnectSpotifyViewModel @Inject constructor(
    private val analyticsDispatcher: AnalyticsDispatcher,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher,
    private val workManager: WorkManager,
    private val sessionRepository: SessionRepository,
    private val userProfileRepository: UserProfileRepository
) : LibzyViewModel<ConnectSpotifyUiState, ConnectSpotifyUiEvent>() {

    override val initialUiState = ConnectSpotifyUiState(loading = true, showSyncProgress = false)

    init {
        viewModelScope.launch {
            presentLibrarySyncProgress()
        }
        viewModelScope.launch {
            handleLibrarySyncSuccess()
        }
    }

    private suspend fun presentLibrarySyncProgress() {
        workManager
            .getWorkInfosByTagLiveData(INITIAL_LIBRARY_SYNC_WORK_TAG)
            .asFlow()
            .onEach {
                updateUiState {
                    copy(loading = false)
                }
            }
            .flatten()
            .map { it.state }
            .collect { librarySyncState ->
                updateUiState {
                    copy(showSyncProgress = !librarySyncState.isFinished || sessionRepository.isSpotifyConnected())
                }

                if (librarySyncState == FAILED || librarySyncState == CANCELLED) {
                    produceUiEvent(SPOTIFY_SYNC_FAILED)
                }
            }
    }

    private suspend fun handleLibrarySyncSuccess() {
        sessionRepository.spotifyConnectedState.collect { spotifyConnected ->
            if (spotifyConnected) {
                produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_CONNECTED)
                workManager.enqueuePeriodicLibrarySync(existingWorkPolicy = KEEP, withInitialDelay = true)
            }
        }
    }

    fun sendScreenViewAnalyticsEvent() {
        analyticsDispatcher.sendViewConnectSpotifyScreenEvent()
    }

    fun onConnectSpotifyClick() {
        viewModelScope.launch {
            analyticsDispatcher.sendClickConnectSpotifyEvent(userProfileRepository.getSpotifyUserId())

            when (spotifyAuthDispatcher.requestAuthorization(withTimeout = false) { setShowDialog(true) }) {
                is SpotifyAuthResult.Failure -> produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_AUTHORIZATION_FAILED)
                is SpotifyAuthResult.Success -> startInitialLibrarySync()
            }
        }
    }

    private fun startInitialLibrarySync() {
        if (uiState.showSyncProgress) return

        viewModelScope.launch {
            val workRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                .setInputData(workDataOf(LibrarySyncWorker.IS_INITIAL_SYNC to true))
                .addTag(INITIAL_LIBRARY_SYNC_WORK_TAG)
                .build()

            workManager.enqueueUniqueWork(
                LibrarySyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            updateUiState {
                copy(showSyncProgress = true)
            }
        }
    }

    companion object {
        private const val INITIAL_LIBRARY_SYNC_WORK_TAG = "initial_library_sync"
    }
}
