package io.libzy.ui.connect

import androidx.work.WorkInfo
import androidx.work.WorkManager

/**
 * @property libraryScanState The current state of the initial Spotify library scan [WorkManager] operation,
 *                            or null if the operation has not yet been enqueued.
 */
data class ConnectSpotifyUiState(
    val libraryScanState: WorkInfo.State?
) {
    val libraryScanInProgress = libraryScanState != null && !libraryScanState.isFinished
}

enum class ConnectSpotifyUiEvent {
    SPOTIFY_CONNECTED,
    SPOTIFY_SCAN_FAILED,
    SPOTIFY_AUTHORIZATION_FAILED
}
