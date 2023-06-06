package io.libzy.spotify.auth

import com.spotify.sdk.android.auth.AuthorizationRequest

interface SpotifyAuthClientProxy {

    fun initiateSpotifyAuthRequest(
        callback: SpotifyAuthCallback,
        authOptions: AuthorizationRequest.Builder.() -> AuthorizationRequest.Builder = { this }
    )
}
