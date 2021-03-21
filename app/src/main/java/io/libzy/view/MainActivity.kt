package io.libzy.view

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse.Type.ERROR
import com.spotify.sdk.android.auth.AuthorizationResponse.Type.TOKEN
import io.libzy.LibzyApplication
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.spotify.auth.*
import io.libzy.util.currentTimeSeconds
import timber.log.Timber
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_main.nav_host_fragment as navHost

/**
 * This is the app's one central Activity, as it is a single activity application using the Navigation component
 * library. It contains the NavHostFragment, which handles navigation between every screen in the app.
 *
 * Manages the app-wide background gradient animation.
 *
 * Interacts with Spotify SDK's auth library by opening an authorization activity when requested and sharing the
 * response with the requester, through [SpotifyAuthDispatcher].
 */
class MainActivity : AppCompatActivity(), SpotifyAuthClientProxy {

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

    private lateinit var backgroundGradient: AnimationDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as LibzyApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        spotifyAuthDispatcher.authClientProxy = this
    }

    override fun onStart() {
        super.onStart()
        initializeBackgroundAnimation() // TODO: does this need to be in onStart?
    }

    private fun initializeBackgroundAnimation() {
        navHost.setBackgroundResource(R.drawable.bkg_gradient_anim)
        if (navHost.background !is AnimationDrawable) throw IllegalStateException("Invalid app background resource")
        backgroundGradient = navHost.background as AnimationDrawable
        backgroundGradient.setEnterFadeDuration(resources.getInteger(R.integer.bkg_gradient_anim_fade_in_duration))
        backgroundGradient.setExitFadeDuration(resources.getInteger(R.integer.bkg_gradient_anim_transition_duration))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) backgroundGradient.start()
        else backgroundGradient.stop()
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
