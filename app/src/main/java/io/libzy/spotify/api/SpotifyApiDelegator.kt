package io.libzy.spotify.api

import android.content.SharedPreferences
import com.adamratzman.spotify.SpotifyApiOptions
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyClientApiBuilder
import com.adamratzman.spotify.SpotifyException.AuthenticationException
import com.adamratzman.spotify.SpotifyException.BadRequestException
import com.adamratzman.spotify.SpotifyException.ParseException
import com.adamratzman.spotify.SpotifyException.ReAuthenticationNeededException
import com.adamratzman.spotify.SpotifyException.TimeoutException
import com.adamratzman.spotify.SpotifyUserAuthorization
import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.AudioFeatures
import com.adamratzman.spotify.models.PlayHistory
import com.adamratzman.spotify.models.SavedAlbum
import com.adamratzman.spotify.models.Track
import io.libzy.persistence.prefs.SharedPrefKeys
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.util.currentTimeSeconds
import io.libzy.util.handle
import io.libzy.util.handleAny
import io.libzy.util.unwrap
import io.libzy.util.wrapResultForOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyApiDelegator @Inject constructor(
    private val sharedPrefs: SharedPreferences,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher,
    applicationScope: CoroutineScope
) {
    companion object {
        // the lower of Spotify's limits for how many items are available from an endpoint
        private const val API_ITEM_LIMIT_LOW = 50

        // a limit to maximize the number of items available from Spotify's paging endpoints which use the lower max
        // limit -- this works by requesting the next page at the highest allowed offset
        private const val API_ITEM_LIMIT_LOW_MAX_PAGING = API_ITEM_LIMIT_LOW - 1
    }

    // do not access the delegate directly -- use getApiDelegate() which initializes it if null
    private var _apiDelegate: SpotifyClientApi? = null

    init {
        applicationScope.launch {
            createApiDelegateIfTokenAvailable()
        }
    }

    // TODO: make this thread-safe by holding a lock during execution,
    //  in case it is called again while we are still creating the delegate,
    //  as to note start creating a new one
    private suspend fun getApiDelegate() =
        _apiDelegate ?: createApiDelegateIfTokenAvailable() ?: createApiDelegateWithNewToken()

    private suspend fun createApiDelegateIfTokenAvailable(): SpotifyClientApi? {
        with(sharedPrefs) {
            val savedAccessToken = getString(SharedPrefKeys.SPOTIFY_AUTH_TOKEN, null)
            val tokenExpirationTimestamp = getLong(SharedPrefKeys.SPOTIFY_AUTH_EXPIRATION_TIMESTAMP, 0)
            if (savedAccessToken != null && currentTimeSeconds() < tokenExpirationTimestamp) {
                _apiDelegate = createApiDelegate(savedAccessToken)
            }
        }
        return _apiDelegate
    }

    private suspend fun createApiDelegateWithNewToken(): SpotifyClientApi {
        val newAccessToken = spotifyAuthDispatcher.requestAuthorization()
        val delegate = createApiDelegate(newAccessToken.token)
        _apiDelegate = delegate
        return delegate
    }

    private suspend fun createApiDelegate(accessToken: String): SpotifyClientApi {
        val apiAuthorization = SpotifyUserAuthorization(tokenString = accessToken)
        val apiOptions = SpotifyApiOptions(automaticRefresh = false, allowBulkRequests = true, testTokenValidity = false)
        return SpotifyClientApiBuilder(authorization = apiAuthorization, options = apiOptions).build()
    }

    suspend fun fetchPlayHistory(): List<PlayHistory> = doSafeApiCall {
        getApiDelegate().player.getRecentlyPlayed(API_ITEM_LIMIT_LOW).items
    }

    suspend fun fetchTopTracks(timeRange: ClientPersonalizationApi.TimeRange): List<Track> = doSafeApiCall {
        getApiDelegate().personalization
            .getTopTracks(API_ITEM_LIMIT_LOW_MAX_PAGING, timeRange = timeRange)
            .getAllItems().filterNotNull()
    }

    suspend fun fetchAllSavedAlbums(): List<SavedAlbum> = doSafeApiCall {
        getApiDelegate().library.getSavedAlbums().getAllItems().filterNotNull()
    }

    suspend fun fetchArtists(ids: Collection<String>): List<Artist?> = doSafeApiCall {
        getApiDelegate().artists.getArtists(*ids.toTypedArray())
    }

    suspend fun fetchAudioFeaturesOfTracks(ids: Collection<String>): List<AudioFeatures?> = doSafeApiCall {
        getApiDelegate().tracks.getAudioFeatures(*ids.toTypedArray())
    }

    suspend fun fetchProfileInfo() = doSafeApiCall {
        getApiDelegate().users.getClientProfile()
    }

    // TODO: remove num503s param when Spotify's issue with frequent 503s is resolved
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
            wrapResultForOutput {
                apiCall()
            }.handleAny(AuthenticationException::class, ReAuthenticationNeededException::class) {
                retryCallWithNewToken()
            }.handle(BadRequestException::class) { e ->
                if (e.statusCode == 401) retryCallWithNewToken()
                else e.statusCode.let { errorCode ->
                    if (errorCode != null && errorCode in 500..599) {
                        Timber.w(e, "API call failed due to server error $errorCode, trying one more time...")
                        apiCall()
                    }
                    else throw e
                }
            }.handle(ParseException::class) { e ->
                Timber.w(e, "API call failed due to server error 503 (unwrapped to ParseException), trying one more time...")
                // this is a temporary workaround for Spotify sometimes sending 503 errors during library refresh
                if (num503s < 10) doSafeApiCall(num503s + 1, apiCall)
                else throw e
            }.handle(TimeoutException::class) { e ->
                Timber.w(e, "API call failed due to timing out, trying one more time...")
                apiCall()
            }.unwrap()
        }
    }

}
