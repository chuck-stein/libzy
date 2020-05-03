package com.chuckstein.libzy.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.chuckstein.libzy.R
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

// TODO: determine whether it's okay for this to be singleton, or if using dependency injection would be better
object SpotifyAuthManager {

    private const val CONNECT_SPOTIFY_REQUEST_CODE = 1104
    private const val REFRESH_SPOTIFY_TOKEN_REQUEST_CODE = 1105

    fun connectSpotify(contextActivity: Activity) {
        requestAuthorization(contextActivity, CONNECT_SPOTIFY_REQUEST_CODE)
    }

    fun refreshSpotifyToken(contextActivity: Activity) {
        requestAuthorization(contextActivity, REFRESH_SPOTIFY_TOKEN_REQUEST_CODE)
    }

    fun isSpotifyAuthRequest(requestCode: Int) =
        requestCode == CONNECT_SPOTIFY_REQUEST_CODE || requestCode == REFRESH_SPOTIFY_TOKEN_REQUEST_CODE

    fun isConnectSpotifyRequest(requestCode: Int) = requestCode == CONNECT_SPOTIFY_REQUEST_CODE

    fun isRefreshSpotifyTokenRequest(requestCode: Int) = requestCode == REFRESH_SPOTIFY_TOKEN_REQUEST_CODE

    private fun requestAuthorization(contextActivity: Activity, requestCode: Int) {
        AuthorizationClient.openLoginActivity(
            contextActivity,
            requestCode,
            buildAuthRequest(contextActivity)
        )
    }

    private fun buildAuthRequest(context: Context) =
        AuthorizationRequest.Builder(
            // TODO: if client_id isn't used anywhere else, remove it from strings.xml and make it a static const
            context.getString(R.string.spotify_client_id),
            AuthorizationResponse.Type.TOKEN,
            getRedirectUri(context).toString()
        )
            .setScopes(arrayOf("user-library-read")) // TODO: determine which scopes I need
            .build()

    private fun getRedirectUri(context: Context) =
        Uri.Builder()
            .scheme(context.getString(R.string.spotify_auth_redirect_scheme))
            .authority(context.getString(R.string.spotify_auth_redirect_host))
            .build()

}