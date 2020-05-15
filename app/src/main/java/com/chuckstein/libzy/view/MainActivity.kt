package com.chuckstein.libzy.view

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.LibzyApplication
import com.chuckstein.libzy.common.currentTimeSeconds
import com.chuckstein.libzy.spotify.auth.*
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
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
        private val AUTH_SCOPES = arrayOf("user-library-read", "app-remote-control")
    }

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
                AuthorizationResponse.Type.TOKEN ->
                    onSpotifyAuthSuccess(SpotifyAccessToken(response.accessToken, response.expiresIn))
                AuthorizationResponse.Type.ERROR -> onSpotifyAuthFailure(response.error)
                else -> onSpotifyAuthFailure("Authorization was prematurely cancelled")
            }
        }
    }

    private fun onSpotifyAuthSuccess(accessToken: SpotifyAccessToken) {
        spotifyAuthCallback?.onSuccess(accessToken)
        saveAccessToken(accessToken)
    }

    private fun saveAccessToken(accessToken: SpotifyAccessToken) {
        val spotifyPrefs = getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        with(spotifyPrefs.edit()) {
            putString(getString(R.string.spotify_access_token_key), accessToken.token)
            putInt(getString(R.string.spotify_token_expiry_key), currentTimeSeconds() + accessToken.expiresIn)
            apply()
        }
    }

    private fun onSpotifyAuthFailure(reason: String) {
        val exception = SpotifyAuthException("Error performing Spotify authorization: $reason")
        spotifyAuthCallback?.onFailure(exception)
    }

    override fun initiateAuthRequest(callback: SpotifyAuthCallback) {
        spotifyAuthCallback = callback
        AuthorizationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, buildAuthRequest())
    }

    private fun buildAuthRequest() =
        AuthorizationRequest.Builder(
            getString(R.string.spotify_client_id),
            AuthorizationResponse.Type.TOKEN,
            getString(R.string.spotify_auth_redirect_uri)
        )
            .setScopes(AUTH_SCOPES)
            .build()

}