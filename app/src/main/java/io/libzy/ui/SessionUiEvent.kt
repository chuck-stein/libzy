package io.libzy.ui

import com.spotify.sdk.android.auth.AuthorizationRequest

sealed interface SessionUiEvent {
    data class SpotifyAuthRequest(val request: AuthorizationRequest) : SessionUiEvent
}
