package io.libzy.spotify.api

import android.content.Context
import android.content.SharedPreferences
import com.adamratzman.spotify.*
import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi
import com.adamratzman.spotify.models.*
import io.libzy.R
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.util.currentTimeSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyApiDelegator @Inject constructor(
    private val context: Context,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher
) {
    companion object {
        // the lower of Spotify's limits for how many items are available from an endpoint
        private const val API_ITEM_LIMIT_LOW = 50

        // the higher of Spotify's limits for how many items are available from an endpoint
        private const val API_ITEM_LIMIT_HIGH = 100

        // a limit to maximize the number of items available from Spotify's paging endpoints which use the lower max
        // limit -- this works by requesting the next page at the highest allowed offset
        private const val API_ITEM_LIMIT_LOW_MAX_PAGING = API_ITEM_LIMIT_LOW - 1
    }

    // do not access the delegate directly -- use getApiDelegate() which initializes it if null
    private var _apiDelegate: SpotifyClientApi? = null

    init {
        createApiDelegateIfTokenAvailable()
    }

    // TODO: make this thread-safe by holding a lock during execution,
    //  in case it is called again while we are still creating the delegate,
    //  as to note start creating a new one
    private suspend fun getApiDelegate() =
        _apiDelegate ?: createApiDelegateIfTokenAvailable() ?: createApiDelegateWithNewToken()

    private fun createApiDelegateIfTokenAvailable(): SpotifyClientApi? {
        val spotifyPrefs: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.spotify_prefs_name),
            Context.MODE_PRIVATE
        )
        val accessTokenKey: String = context.getString(R.string.spotify_access_token_key)
        val expirationKey: String = context.getString(R.string.spotify_token_expiration_key)
        val savedAccessToken = spotifyPrefs.getString(accessTokenKey, null)
        val savedTokenExpiration = spotifyPrefs.getInt(expirationKey, 0)
        if (savedAccessToken != null && currentTimeSeconds() < savedTokenExpiration) {
            _apiDelegate = createApiDelegate(savedAccessToken)
        }
        return _apiDelegate
    }

    private suspend fun createApiDelegateWithNewToken(): SpotifyClientApi {
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

    suspend fun getUserId() = getApiDelegate().userId

    // TODO: rename all these functions to "fetchXXXXX" instead of "getXXXXX"
    suspend fun getPlayHistory(): List<PlayHistory> = doSafeApiCall {
        getApiDelegate().player.getRecentlyPlayed(API_ITEM_LIMIT_LOW).suspendQueue().items
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

    suspend fun getProfileInformation() = doSafeApiCall {
        getApiDelegate().users.getClientProfile().suspendQueue()
    }

    // TODO: remove num503s param when the source of the issue is fixed
    private suspend fun <T> doSafeApiCall(num503s: Int = 0, apiCall: suspend () -> T): T {

        suspend fun retryCallWithNewToken(): T {
            val newAccessToken = spotifyAuthDispatcher.requestAuthorization()
            getApiDelegate().updateToken {
                accessToken = newAccessToken.token
                expiresIn = newAccessToken.expiresIn // duration of validity in seconds
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
                else e.statusCode.let { errorCode ->
                    if (errorCode != null && errorCode >= 500 && errorCode < 600) {
                        Timber.w(e, "API call failed due to server error $errorCode, trying one more time...")
                        apiCall()
                    }
                    else throw e
                }
            } catch (e: SpotifyException.ParseException) {
                Timber.w(e, "API call failed due to server error 503 (unwrapped to ParseException), trying one more time...")
                // TODO: resolve this temporary workaround for Spotify sometimes sending 503 during library refresh
                if (num503s < 10) doSafeApiCall(num503s + 1, apiCall)
                else throw e
            } catch (e: SpotifyException.TimeoutException) {
                Timber.w(e, "API call failed due to timing out, trying one more time...")
                apiCall()
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
