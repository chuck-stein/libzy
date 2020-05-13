package com.chuckstein.libzy.network

import android.content.Context
import android.content.SharedPreferences
import com.adamratzman.spotify.*
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SavedAlbum
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.currentTimeSeconds
import com.chuckstein.libzy.network.auth.SpotifyAuthDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO: handle the case where getApiDelegate() is called, starts initializing the client, then is called again from elsewhere -- don't want to start initializing again
@Singleton
class SpotifyApiDelegator @Inject constructor(
    applicationContext: Context,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher
) {

    // do not access the delegate directly -- use getApiDelegate() which initializes it if null
    private var _apiDelegate: SpotifyClientApi? = null

    init {
        val spotifyPrefs: SharedPreferences = applicationContext.getSharedPreferences(
            applicationContext.getString(R.string.spotify_prefs_name),
            Context.MODE_PRIVATE
        )
        val accessTokenKey: String = applicationContext.getString(R.string.spotify_access_token_key)
        val expiryKey: String = applicationContext.getString(R.string.spotify_token_expiry_key)
        val savedAccessToken = spotifyPrefs.getString(accessTokenKey, null)
        val savedTokenExpiry = spotifyPrefs.getInt(expiryKey, 0)
        if (savedAccessToken != null && currentTimeSeconds() < savedTokenExpiry) {
            _apiDelegate = createApiDelegate(savedAccessToken)
        }
    }

    private suspend fun getApiDelegate() = _apiDelegate ?: initApiDelegateWithNewToken()

    private suspend fun initApiDelegateWithNewToken(): SpotifyClientApi {
        val newAccessToken = spotifyAuthDispatcher.requestAuthorization()
        val delegate = createApiDelegate(newAccessToken)
        _apiDelegate = delegate
        return delegate
    }

    private fun createApiDelegate(accessToken: String): SpotifyClientApi {
        val apiAuthorization = SpotifyUserAuthorizationBuilder(tokenString = accessToken).build()
        val apiOptions = SpotifyApiOptionsBuilder(automaticRefresh = false, testTokenValidity = false).build()
        return SpotifyClientApiBuilder(authorization = apiAuthorization, options = apiOptions).build()
    }

    private suspend fun <T> doSafeApiCall(apiCall: suspend () -> T): T = withContext(Dispatchers.IO) {
        try {
            apiCall()
        } catch (e: SpotifyException.BadRequestException) {
            if (e.statusCode?.equals(401) == true) {
                val newAccessToken = spotifyAuthDispatcher.requestAuthorization()
                getApiDelegate().updateTokenWith(newAccessToken)
                apiCall()
            } else throw e
        }
    }

    suspend fun getAllSavedAlbums(): List<SavedAlbum> = doSafeApiCall {
        getApiDelegate().library.getSavedAlbums().getAllItems().suspendQueue()
    }

    suspend fun getArtists(ids: List<String>): List<Artist?> = doSafeApiCall {
        getApiDelegate().artists.getArtists(*ids.toTypedArray()).suspendQueue()
    }

}