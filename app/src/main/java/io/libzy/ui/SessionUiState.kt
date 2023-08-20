package io.libzy.ui

data class SessionUiState(
    val isSpotifyConnected: Boolean,
    val isOnboardingCompleted: Boolean,
    val areEnoughAlbumsSaved: Boolean,
    val loading: Boolean = false
)
