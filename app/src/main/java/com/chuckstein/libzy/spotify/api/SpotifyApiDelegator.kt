package com.chuckstein.libzy.spotify.api

import android.content.Context
import android.content.SharedPreferences
import com.adamratzman.spotify.*
import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi
import com.adamratzman.spotify.models.*
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
    context: Context,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher
) {
    companion object {
        // the lower of Spotify's limits for how many items are available from an endpoint
        private const val API_ITEM_LIMIT_LOW = 50

        // the higher of Spotify's limits for how many items are available from an endpoint
        private const val API_ITEM_LIMIT_HIGH = 100

        // a limit to maximize the number of items available from Spotify's paging endpoints which use the lower max
        // limit -- this works by requesting the next page at the highest allowed offset
        // TODO: verify this works and is the best approach
        private const val API_ITEM_LIMIT_LOW_MAX_PAGING = API_ITEM_LIMIT_LOW - 1
    }

    // do not access the delegate directly -- use getApiDelegate() which initializes it if null
    private var _apiDelegate: SpotifyClientApi? = null

    init {
        val spotifyPrefs: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.spotify_prefs_name),
            Context.MODE_PRIVATE
        )
        val accessTokenKey: String = context.getString(R.string.spotify_access_token_key)
        val expiryKey: String = context.getString(R.string.spotify_token_expiry_key)
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

    suspend fun getPlayHistory(): List<PlayHistory> = doSafeApiCall {
        getApiDelegate().player.getRecentlyPlayed(API_ITEM_LIMIT_LOW).suspendQueue().items
    }

    // TODO: delete if unused
    suspend fun getTopArtists(timeRange: ClientPersonalizationApi.TimeRange): List<Artist> = doSafeApiCall {
        getApiDelegate().personalization.getTopArtists(API_ITEM_LIMIT_LOW_MAX_PAGING, timeRange = timeRange).suspendQueueAll() // TODO: fix apparent JSON parsing issue w/ max paging?
    }

    suspend fun getTopTracks(timeRange: ClientPersonalizationApi.TimeRange): List<Track> = doSafeApiCall {
        getApiDelegate().personalization.getTopTracks(API_ITEM_LIMIT_LOW_MAX_PAGING, timeRange = timeRange).suspendQueueAll() // TODO: fix apparent JSON parsing issue w/ max paging?
    }

    suspend fun getAllSavedAlbums(): List<SavedAlbum> = doSafeApiCall {
        getApiDelegate().library.getSavedAlbums().suspendQueueAll()
    }

    suspend fun getArtists(ids: Collection<String>): List<Artist?> =
        getBatchedItems(ids, getApiDelegate().artists::getArtists, API_ITEM_LIMIT_LOW)

    // TODO: fix rate limiting
    suspend fun getAudioFeaturesOfTracks(ids: Collection<String>): List<AudioFeatures?> =
        getBatchedItems(ids, getApiDelegate().tracks::getAudioFeatures, API_ITEM_LIMIT_HIGH)

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

    // TODO: delegate batching responsibility to SpotifyClientApi once adamint fixes bulk request bug w/ empty JSON (allegedly done as of 3.1.0?)
    private suspend fun <T> getBatchedItems(
        ids: Collection<String>,
        endpoint: (Array<out String>) -> SpotifyRestAction<List<T?>>,
        batchSize: Int
    ): List<T?> {
        val items = mutableListOf<T?>()
        val batches = ids.chunked(batchSize)
        for (batch in batches) {
            val batchItems = doSafeApiCall { endpoint(*batch.toTypedArray()).suspendQueue() }
            items.addAll(batchItems)
        }
        return items
    }

}