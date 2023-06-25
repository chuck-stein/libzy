package io.libzy.ui

data class SessionUiState(
    val isSpotifyConnected: Boolean,
    val isOnboardingCompleted: Boolean,
    val loading: Boolean = false
)
