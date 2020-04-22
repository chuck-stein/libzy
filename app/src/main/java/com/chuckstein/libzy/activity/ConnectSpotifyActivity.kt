package com.chuckstein.libzy.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.chuckstein.libzy.R
import com.chuckstein.libzy.extension.initializeBackground
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlin.math.roundToInt

class ConnectSpotifyActivity : AppCompatActivity() {

    companion object {
        private val TAG = ConnectSpotifyActivity::class.java.simpleName
        private const val SPOTIFY_AUTH_REQUEST_CODE = 1104
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_spotify)
        initializeBackground()
    }

    fun connectSpotifyButtonClicked(view: View) {
        AuthorizationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, buildAuthRequest())
    }

    private fun buildAuthRequest(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            // TODO: if client_id isn't used anywhere else, remove it from strings.xml and make it local
            getString(R.string.spotify_client_id),
            AuthorizationResponse.Type.TOKEN,
            getRedirectUri().toString()
        )
            .setScopes(arrayOf("user-library-read")) // TODO: determine which scopes I need
            .build()
    }

    private fun getRedirectUri(): Uri {
        return Uri.Builder()
            .scheme(getString(R.string.spotify_auth_redirect_scheme))
            .authority(getString(R.string.spotify_auth_redirect_host))
            .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == SPOTIFY_AUTH_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    // Auth flow was successful
                    saveToken(response.accessToken, response.expiresIn)
                    startActivity(Intent(this, SelectGenresActivity::class.java))
                }
                AuthorizationResponse.Type.ERROR -> {
                    // Auth flow was unsuccessful
                    Log.d(TAG, "Error performing Spotify authorization: ${response.error}")
                    // TODO: try to resolve different errors
                }
                else -> {
                    // Auth flow was most likely cancelled
                    Log.w(TAG,"Spotify authorization failed without an error, most likely cancelled")
                }
            }
        }
    }

    // TODO: move this to a ViewModel?
    private fun saveToken(accessToken: String, expiresIn: Int) {
        Log.d(TAG, "got access token which expires in $expiresIn seconds");
        val sharedPref =
            getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        val currTime = (System.currentTimeMillis() / 1000.0).roundToInt()
        with(sharedPref.edit()) {
            putString(getString(R.string.spotify_access_token_key), accessToken)
            putInt(getString(R.string.spotify_token_expiry_key), currTime + expiresIn)
            // set flag that we've connected before, so don't ask on subsequent sessions
            if (!sharedPref.getBoolean(getString(R.string.spotify_connected_key), false)) {
                putBoolean(getString(R.string.spotify_connected_key), true)
            }
            apply()
        }
    }

}
