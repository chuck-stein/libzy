package io.libzy.ui.connect

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.database.SharedPrefKeys
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthException
import io.libzy.ui.common.ScreenViewModel
import io.libzy.util.handle
import io.libzy.util.unwrap
import io.libzy.util.wrapResult
import io.libzy.work.LibrarySyncWorker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConnectSpotifyViewModel @Inject constructor(
    appContext: Context,
    private val analyticsDispatcher: AnalyticsDispatcher,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher
) : ScreenViewModel<ConnectSpotifyUiState, ConnectSpotifyUiEvent>() {

    override val initialUiState = ConnectSpotifyUiState(libraryScanState = null)

    private val workManager = WorkManager.getInstance(appContext)

    private val spotifyPrefs =
        appContext.getSharedPreferences(appContext.getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            processLibraryScanUpdates()
        }
    }

    private suspend fun processLibraryScanUpdates() {
        workManager.getWorkInfosByTagLiveData(LIBRARY_SCAN_WORK_TAG).asFlow()
            .collect { libraryScanWorkInfos ->

                val libraryScanState = libraryScanWorkInfos.firstOrNull()?.state

                updateUiState {
                    copy(libraryScanState = libraryScanState)
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
        val currentlyConnectedUserId = spotifyPrefs.getString(SharedPrefKeys.SPOTIFY_USER_ID, null)
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
        }
    }

    private fun spotifyConnected() = spotifyPrefs.getBoolean(SharedPrefKeys.SPOTIFY_CONNECTED, false)

    companion object {
        private const val LIBRARY_SCAN_WORK_TAG = "initial_library_scan"
    }
}
