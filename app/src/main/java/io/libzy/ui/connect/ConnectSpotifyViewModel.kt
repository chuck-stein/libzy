package io.libzy.ui.connect

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.persistence.prefs.SharedPrefKeys
import io.libzy.persistence.prefs.getSharedPrefs
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthException
import io.libzy.ui.common.LibzyViewModel
import io.libzy.util.handle
import io.libzy.util.unwrap
import io.libzy.util.wrapResult
import io.libzy.work.LibrarySyncWorker
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConnectSpotifyViewModel @Inject constructor(
    private val analyticsDispatcher: AnalyticsDispatcher,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher,
    private val workManager: WorkManager,
    private val sharedPrefs: SharedPreferences
) : LibzyViewModel<ConnectSpotifyUiState, ConnectSpotifyUiEvent>() {

    override val initialUiState = ConnectSpotifyUiState(
        libraryScanInProgress = sharedPrefs.getBoolean(SharedPrefKeys.SPOTIFY_INITIAL_SCAN_IN_PROGRESS, false)
    )

    init {
        viewModelScope.launch {
            processLibraryScanUpdates()
        }
    }

    private suspend fun processLibraryScanUpdates() {
        workManager.getWorkInfosByTagLiveData(LIBRARY_SCAN_WORK_TAG).asFlow()
            .map { it.firstOrNull()?.state }.filterNotNull().collect { libraryScanState ->

                updateUiState {
                    copy(libraryScanInProgress = !libraryScanState.isFinished)
                }

                if (libraryScanState == WorkInfo.State.SUCCEEDED && spotifyConnected()) {
                    produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_CONNECTED)
                } else if (libraryScanState == WorkInfo.State.FAILED || libraryScanState == WorkInfo.State.CANCELLED) {
                    produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_SCAN_FAILED)
                }
            }
    }

    fun sendScreenViewAnalyticsEvent() {
        analyticsDispatcher.sendViewConnectSpotifyScreenEvent()
    }

    fun onConnectSpotifyClick() {
        val currentlyConnectedUserId = sharedPrefs.getString(SharedPrefKeys.SPOTIFY_USER_ID, null)
        analyticsDispatcher.sendClickConnectSpotifyEvent(currentlyConnectedUserId)

        if (spotifyConnected()) {
            produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_CONNECTED)
            return
        }

        viewModelScope.launch {
            wrapResult {
                spotifyAuthDispatcher.requestAuthorization(withTimeout = false)
            }.handle(SpotifyAuthException::class) {
                produceUiEvent(ConnectSpotifyUiEvent.SPOTIFY_AUTHORIZATION_FAILED)
            }.unwrap {
                scanLibrary()
            }
        }
    }

    private fun spotifyConnected() = sharedPrefs.getBoolean(SharedPrefKeys.SPOTIFY_CONNECTED, false)

    private fun scanLibrary() {
        if (uiState.value.libraryScanInProgress) return

        viewModelScope.launch {
            val workRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                .setInputData(workDataOf(LibrarySyncWorker.IS_INITIAL_SCAN to true))
                .addTag(LIBRARY_SCAN_WORK_TAG)
                .build()

            workManager.enqueueUniqueWork(
                LibrarySyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            updateUiState {
                copy(libraryScanInProgress = true)
            }

            sharedPrefs.edit {
                putBoolean(SharedPrefKeys.SPOTIFY_INITIAL_SCAN_IN_PROGRESS, true)
            }
        }
    }

    companion object {
        private const val LIBRARY_SCAN_WORK_TAG = "initial_library_scan"
    }
}
