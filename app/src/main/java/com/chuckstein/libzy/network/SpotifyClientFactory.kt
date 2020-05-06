package com.chuckstein.libzy.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.currentTimeSeconds
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
TODO:
 - factor in app session buffer time, which is the same tokenShouldRefresh() check as done in MainActivity, so do that in only once place
 - ensure only SpotifyClientFactory can instantiate SpotifyClients
 - handle the case where getClient() is called, starts initializing the client, then is called again from elsewhere -- don't want to start initializing again
 - how will I update the token of an already-initialized SpotifyClient, for example when the user has been using the app until token expiry so we get a new one
 - determine best timeout period (take into account slow connections)
 - should it be called "factory" or "singleton"? maybe "loader"?
 - should the constructor param be private val? or do initialization in init?
 - if all else fails, make this a singleton with an updateAccessToken(String, Int) method and when an instance is requested it waits 5ish seconds until receiving a callback tht token is updated, otherwise times out
*/
class SpotifyClientFactory(context: Context) {

    companion object {
        private val TAG = SpotifyClientFactory::class.java.simpleName
        private const val INITIALIZATION_TIMEOUT_MILLIS = 5000L
    }

    private var spotifyClient: SpotifyClient? = null

    private val spotifyPrefs: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
    private val accessTokenKey: String = context.getString(R.string.spotify_access_token_key)
    private val expiryKey: String = context.getString(R.string.spotify_token_expiry_key)

    suspend fun getClient() = spotifyClient ?: initializeClient()

    private suspend fun initializeClient(): SpotifyClient {
        val savedAccessToken = spotifyPrefs.getString(accessTokenKey, null)
        val savedTokenExpiry = spotifyPrefs.getInt(expiryKey, 0)
        // TODO: uncomment this if block when done testing
        val newClient = if (savedAccessToken != null && currentTimeSeconds() < savedTokenExpiry) {
            SpotifyClient(savedAccessToken)
        } else {
            val newAccessToken = awaitNewAccessToken()
            SpotifyClient(newAccessToken)
        }
        spotifyClient = newClient
        return newClient
    }

    private suspend fun awaitNewAccessToken(): String {
        // declare the owners of callbacks which will need to be cleaned up later
        val timeoutHandler = Handler()
        lateinit var failWithTimeout: Runnable // TODO: is "lateinit" best practice here?
        lateinit var spotifyPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener // TODO: is "lateinit" best practice here?

        // TODO: determine whether this wait is long enough that there should be a different CoroutineScope, maybe Dispatchers.IO
        // suspend coroutine to register callbacks and await their result
        val newAccessToken = suspendCancellableCoroutine<String> { continuation ->

            // listen to SharedPreferences for a new access token being saved
            spotifyPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                if (key == accessTokenKey) {
                    val newToken = prefs.getString(accessTokenKey, null)
                    if (newToken != null) continuation.resume(newToken)
                }
            }
            spotifyPrefs.registerOnSharedPreferenceChangeListener(spotifyPrefsListener)

            // set a timeout to throw an exception if no new access token is saved in a reasonable amount of time
            failWithTimeout = Runnable {
                if (!continuation.isCompleted) {
                    continuation.resumeWithException(
                        // TODO: use custom SpotifyClientInitializationException instead?
                        TimeoutException(
                            "Failed to initialize the SpotifyClient -- timed out while waiting for a new access token"
                        )
                    )
                }
            }
            timeoutHandler.postDelayed(failWithTimeout, INITIALIZATION_TIMEOUT_MILLIS)

            // if coroutine is cancelled before continuing, clean up the callbacks
            continuation.invokeOnCancellation {
                spotifyPrefs.unregisterOnSharedPreferenceChangeListener(spotifyPrefsListener)
                timeoutHandler.removeCallbacks(failWithTimeout)
            }
        }
        // clean up the callbacks after coroutine continues, so they are not called later unnecessarily
        spotifyPrefs.unregisterOnSharedPreferenceChangeListener(spotifyPrefsListener)
        timeoutHandler.removeCallbacks(failWithTimeout)
        return newAccessToken
    }

}