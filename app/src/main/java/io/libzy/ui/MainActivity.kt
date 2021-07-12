package io.libzy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse.Type.ERROR
import com.spotify.sdk.android.auth.AuthorizationResponse.Type.TOKEN
import io.libzy.LibzyApplication
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.spotify.auth.SpotifyAccessToken
import io.libzy.spotify.auth.SpotifyAuthCallback
import io.libzy.spotify.auth.SpotifyAuthClientProxy
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthException
import io.libzy.util.currentTimeSeconds
import timber.log.Timber
import javax.inject.Inject

/**
 * This is Libzy's one central Activity, as it is a single activity application.
 *
 * Sets the UI content to [LibzyNavGraph], which encapsulates the app's various composable screens
 * as well as navigation between them.
 *
 * Interacts with the Spotify SDK's auth library by opening an authorization Activity when requested and sharing the
 * response with the requester, through [SpotifyAuthDispatcher].
 */
class MainActivity : ComponentActivity(), SpotifyAuthClientProxy {

    companion object {
        private const val SPOTIFY_AUTH_REQUEST_CODE = 1104
        private val AUTH_SCOPES =
            arrayOf("user-library-read", "app-remote-control", "user-read-recently-played", "user-top-read")
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by viewModels<MainViewModel> { viewModelFactory }

    @Inject
    lateinit var analyticsDispatcher: AnalyticsDispatcher

    @Inject
    lateinit var spotifyAuthDispatcher: SpotifyAuthDispatcher

    private var spotifyAuthCallback: SpotifyAuthCallback? = null

    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as LibzyApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        spotifyAuthDispatcher.authClientProxy = this
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LibzyContent {
                LibzyNavGraph(viewModelFactory)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == SPOTIFY_AUTH_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                TOKEN -> onSpotifyAuthSuccess(SpotifyAccessToken(response.accessToken, response.expiresIn))
                ERROR -> onSpotifyAuthFailure(response.error)
                else -> onSpotifyAuthFailure("Authorization was prematurely cancelled")
            }
        }
    }

    private fun onSpotifyAuthSuccess(accessToken: SpotifyAccessToken) {
        spotifyAuthCallback?.onSuccess(accessToken)
        saveAccessToken(accessToken)
        model.onNewSpotifySession()
        analyticsDispatcher.sendAuthorizeSpotifyConnectionEvent()
    }

    private fun saveAccessToken(accessToken: SpotifyAccessToken) {
        getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE).edit {
            putString(getString(R.string.spotify_access_token_key), accessToken.token)
            putInt(getString(R.string.spotify_token_expiration_key), currentTimeSeconds() + accessToken.expiresIn)
        }
    }

    private fun onSpotifyAuthFailure(reason: String) {
        val exception = SpotifyAuthException("Error performing Spotify authorization: $reason")
        Timber.e(exception)
        spotifyAuthCallback?.onFailure(exception)
    }

    override fun initiateAuthRequest(callback: SpotifyAuthCallback) {
        spotifyAuthCallback = callback
        AuthorizationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, buildAuthRequest())
    }

    private fun buildAuthRequest() =
        AuthorizationRequest.Builder(
            getString(R.string.spotify_client_id),
            TOKEN,
            getString(R.string.spotify_auth_redirect_uri)
        )
            .setScopes(AUTH_SCOPES)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        spotifyAuthDispatcher.authClientProxy = null
    }
}
