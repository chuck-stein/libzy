package com.chuckstein.libzy.view.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.chuckstein.libzy.R
import com.chuckstein.libzy.auth.SpotifyAuthManager
import com.chuckstein.libzy.view.fragment.ConnectSpotifyFragmentDirections
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.android.synthetic.main.activity_main.nav_host_fragment as navHost
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

/**
 * The app's one central Activity, as this is a single activity application using the Navigation component library.
 *
 * Contains the NavHostFragment, which handles navigation between every screen in the app.
 *
 * Manages the app-wide background gradient animation.
 *
 * Interacts with Spotify SDK's auth library by opening an authorization activity when the user's access token
 * should refresh, and handling when an authorization activity returns.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var spotifyPrefs: SharedPreferences

    private lateinit var backgroundGradient: AnimationDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        spotifyPrefs = getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
    }

    @ExperimentalTime
    override fun onStart() {
        super.onStart()

        initializeBackgroundAnimation()

        if (spotifyConnected() && tokenShouldRefresh()) {
            SpotifyAuthManager.refreshSpotifyToken(this)
        }
        // TODO: if spotifyConnected() && !tokenShouldRefresh(), set a timed callback to refresh it if we're still on screen when it's about to expire
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) backgroundGradient.start()
        else backgroundGradient.stop()
    }

    private fun initializeBackgroundAnimation() {
        navHost.setBackgroundResource(R.drawable.bkg_gradient_anim)
        if (navHost.background !is AnimationDrawable) throw IllegalStateException("Invalid app background resource")
        backgroundGradient = navHost.background as AnimationDrawable
        backgroundGradient.setEnterFadeDuration(resources.getInteger(R.integer.bkg_gradient_anim_fade_in_duration))
        backgroundGradient.setExitFadeDuration(resources.getInteger(R.integer.bkg_gradient_anim_transition_duration))
    }

    // TODO: LaunchFragment may also be reading this shared prefs value at the same time, can we do only one call for both?
    private fun spotifyConnected() = spotifyPrefs.getBoolean(getString(R.string.spotify_connected_key), false)

    @ExperimentalTime
    private fun tokenShouldRefresh() = currentTimeSeconds() + appSessionBufferTime() > spotifyTokenExpiry()

    @ExperimentalTime
    private fun appSessionBufferTime() = Duration.convert(10.0, DurationUnit.MINUTES, DurationUnit.SECONDS)

    private fun spotifyTokenExpiry() = spotifyPrefs.getInt(getString(R.string.spotify_token_expiry_key), 0)

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (SpotifyAuthManager.isSpotifyAuthRequest(requestCode)) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    saveToken(response.accessToken, response.expiresIn)
                    if (SpotifyAuthManager.isConnectSpotifyRequest(requestCode)) onSpotifyConnected()
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e(TAG, "Error performing Spotify authorization: ${response.error}")
                    // TODO: notify the user that app cannot continue -- can't make requests without authorization, can't use the app without requests
                    //         - if it was a connect spotify request, leave them on the ConnectSpotify screen so they can try again, maybe add a textview to explain error/try again
                    //         - if it was a refresh token request, move to a screen explaining what went wrong, only let them return after pressing a "try again" button that calls SpotifyAuthManager again and then goes back to previous screen (can't press back button)
                }
                else -> {
                    // Auth flow was most likely cancelled
                    Log.w(TAG, "Spotify authorization failed without an error, most likely cancelled")
                }
            }
        }
    }

    private fun saveToken(accessToken: String, expiresIn: Int) {
        Log.d(TAG, "got access token which expires in $expiresIn seconds");
        with(spotifyPrefs.edit()) {
            putInt(getString(R.string.spotify_token_expiry_key), currentTimeSeconds() + expiresIn)
            putString(getString(R.string.spotify_access_token_key), accessToken)
            apply()
        }
    }

    private fun onSpotifyConnected() {
        Log.d(TAG, "Spotify connected!")
        with(spotifyPrefs.edit()) {
            putBoolean(getString(R.string.spotify_connected_key), true)
            apply()
        }
        val navController = navHost.findNavController()
        if (navController.currentDestination?.id == R.id.connectSpotifyFragment) {
            navController.navigate(
                ConnectSpotifyFragmentDirections.actionConnectSpotifyFragmentToSelectGenresFragment())
        }
    }

    // TODO: break this out into Util singleton if further need arises
    private fun currentTimeSeconds() = (System.currentTimeMillis() / 1000.0).roundToInt()

}
