package com.chuckstein.libzy.spotify.api

import android.content.Context
import android.content.SharedPreferences
import com.adamratzman.spotify.*
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SavedAlbum
import com.adamratzman.spotify.utils.getCurrentTimeMs
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.currentTimeSeconds
import com.chuckstein.libzy.spotify.auth.SpotifyAuthDispatcher
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
    companion object {
        const val API_ARG_LIMIT = 50
    }

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
        val delegate = createApiDelegate(newAccessToken.token)
        _apiDelegate = delegate
        return delegate
    }

    private fun createApiDelegate(accessToken: String): SpotifyClientApi {
        val apiAuthorization = SpotifyUserAuthorizationBuilder(tokenString = accessToken).build()
        val apiOptions = SpotifyApiOptionsBuilder(automaticRefresh = false, testTokenValidity = false).build()
        return SpotifyClientApiBuilder(authorization = apiAuthorization, options = apiOptions).build()
    }

    private suspend fun <T> doSafeApiCall(apiCall: suspend () -> T): T {

        suspend fun retryCallWithNewToken(): T {
            val newAccessToken = spotifyAuthDispatcher.requestAuthorization()
            getApiDelegate().updateToken {
                accessToken = newAccessToken.token
                expiresIn = newAccessToken.expiresIn                                // duration of validity in seconds
                expiresAt = newAccessToken.expiresIn * 1000 + getCurrentTimeMs()    // timestamp of expiry in ms
            }
            return apiCall()
        }

        return withContext(Dispatchers.IO) {
            try {
                apiCall()
            } catch (e: SpotifyException.AuthenticationException) {
                retryCallWithNewToken()
            } catch (e: SpotifyException.BadRequestException) {
                if (e.statusCode == 401) retryCallWithNewToken()
                else throw e
            }
        }
    }

    suspend fun getAllSavedAlbums(): List<SavedAlbum> = doSafeApiCall {
        getApiDelegate().library.getSavedAlbums().getAllItems().suspendQueue()
    }

    suspend fun getArtists(ids: Collection<String>): List<Artist?> =
        getBatchedItems(ids, getApiDelegate().artists::getArtists)

    suspend fun getAlbums(ids: Collection<String>): List<Album?> =
        getBatchedItems(ids, getApiDelegate().albums::getAlbums)

    // TODO: delegate batching responsibility to SpotifyClientApi once adamint fixes bulk request bug w/ empty JSON
    private suspend fun <T> getBatchedItems(
        ids: Collection<String>,
        endpoint: (Array<out String>) -> SpotifyRestAction<List<T?>>
    ): List<T?> {
        val items = mutableListOf<T?>()
        val batches = ids.chunked(API_ARG_LIMIT)
        for (batch in batches) {
            val batchItems = doSafeApiCall { endpoint(*batch.toTypedArray()).suspendQueue() }
            items.addAll(batchItems)
        }
        return items
    }

}