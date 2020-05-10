package com.chuckstein.libzy.view.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.currentTimeSeconds
import com.chuckstein.libzy.network.auth.SpotifyAuthCallback
import com.chuckstein.libzy.network.auth.SpotifyAuthClientProxy
import com.chuckstein.libzy.network.auth.SpotifyAuthDispatcher
import com.chuckstein.libzy.network.auth.SpotifyAuthException
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
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
    }

    private var spotifyAuthCallback: SpotifyAuthCallback? = null

    private lateinit var backgroundGradient: AnimationDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SpotifyAuthDispatcher.authClientProxy = this
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
                AuthorizationResponse.Type.TOKEN -> onSpotifyAuthSuccess(response.accessToken, response.expiresIn)
                AuthorizationResponse.Type.ERROR -> onSpotifyAuthFailure(response.error)
                else -> onSpotifyAuthFailure("Authorization was prematurely cancelled")
            }
        }
    }

    private fun onSpotifyAuthSuccess(accessToken: String, expiresIn: Int) {
        spotifyAuthCallback?.onSuccess(accessToken)
        saveAccessToken(accessToken, expiresIn)
    }

    private fun saveAccessToken(accessToken: String, expiresIn: Int) {
        val spotifyPrefs = getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        with(spotifyPrefs.edit()) {
            putString(getString(R.string.spotify_access_token_key), accessToken)
            putInt(getString(R.string.spotify_token_expiry_key), currentTimeSeconds() + expiresIn)
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
            // TODO: if client_id isn't used anywhere else, remove it from strings.xml and make it a static const
            getString(R.string.spotify_client_id),
            AuthorizationResponse.Type.TOKEN,
            getRedirectUri().toString()
        )
            .setScopes(arrayOf("user-library-read")) // TODO: determine which scopes I need
            .build()

    private fun getRedirectUri() =
        Uri.Builder()
            .scheme(getString(R.string.spotify_auth_redirect_scheme))
            .authority(getString(R.string.spotify_auth_redirect_host))
            .build()

}