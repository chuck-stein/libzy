package io.libzy.ui.connect

import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.repository.SessionRepository
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthException
import io.libzy.ui.common.LibzyViewModel
import io.libzy.util.flatten
import io.libzy.util.handle
import io.libzy.util.unwrap
import io.libzy.util.wrapResult
import io.libzy.work.LibrarySyncWorker
import io.libzy.work.LibrarySyncWorker.Companion.LIBRARY_SYNC_INTERVAL
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConnectSpotifyViewModel @Inject constructor(
    private val analyticsDispatcher: AnalyticsDispatcher,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher,
    private val workManager: WorkManager,
    private val sessionRepository: SessionRepository
) : LibzyViewModel<ConnectSpotifyUiState, ConnectSpotifyUiEvent>() {

    override val initialUiState = ConnectSpotifyUiState(loading = true, librarySyncInProgress = false)

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
                    copy(librarySyncInProgress = !librarySyncState.isFinished)
                }

                if (librarySyncState == WorkInfo.State.FAILED || librarySyncState == WorkInfo.State.CANCELLED) {
                    produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_SYNC_FAILED)
                }
            }
    }

    private suspend fun handleLibrarySyncSuccess() {
        sessionRepository.spotifyConnectedState.collect { spotifyConnected ->
            if (spotifyConnected) {
                produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_CONNECTED)
                enqueuePeriodicLibrarySync()
            }
        }
    }

    fun sendScreenViewAnalyticsEvent() {
        analyticsDispatcher.sendViewConnectSpotifyScreenEvent()
    }

    fun onConnectSpotifyClick() {
        viewModelScope.launch {
            analyticsDispatcher.sendClickConnectSpotifyEvent(sessionRepository.getSpotifyUserId())

            wrapResult {
                spotifyAuthDispatcher.requestAuthorization(withTimeout = false) { setShowDialog(true) }
            }.handle(SpotifyAuthException::class) {
                produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_AUTHORIZATION_FAILED)
            }.unwrap {
                startInitialLibrarySync()
            }
        }
    }

    private fun startInitialLibrarySync() {
        if (uiState.librarySyncInProgress) return

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
                copy(librarySyncInProgress = true)
            }
        }
    }

    private fun enqueuePeriodicLibrarySync() {
        val workRequest =
            PeriodicWorkRequestBuilder<LibrarySyncWorker>(LIBRARY_SYNC_INTERVAL)
                .setInitialDelay(LIBRARY_SYNC_INTERVAL)
                .build()

        workManager.enqueueUniquePeriodicWork(
            LibrarySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        private const val INITIAL_LIBRARY_SYNC_WORK_TAG = "initial_library_sync"
    }
}
