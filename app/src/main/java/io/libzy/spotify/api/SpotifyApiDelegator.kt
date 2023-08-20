package io.libzy.spotify.api

import com.adamratzman.spotify.SpotifyApiOptions
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyClientApiBuilder
import com.adamratzman.spotify.SpotifyException
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
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.libzy.repository.SessionRepository
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthResult
import io.libzy.util.handle
import io.libzy.util.handleAny
import io.libzy.util.unwrap
import io.libzy.util.wrapResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyApiDelegator @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val spotifyAuthDispatcher: SpotifyAuthDispatcher,
    applicationScope: CoroutineScope
) {
    companion object {
        // the lower of Spotify's limits for how many items are available from an endpoint
        const val API_ITEM_LIMIT_LOW = 50

        // a limit to maximize the number of items available from Spotify's paging endpoints which use the lower max
        // limit -- this works by requesting the next page at the highest allowed offset
        private const val API_ITEM_LIMIT_LOW_MAX_PAGING = API_ITEM_LIMIT_LOW - 1
    }

    // do not access the delegate directly -- use getApiDelegate() which initializes it if null
    private var _apiDelegate: SpotifyClientApi? = null

    private val apiDelegateMutex = Mutex()

    init {
        applicationScope.launch {
            createApiDelegateIfTokenAvailable()
        }
    }

    private suspend fun getApiDelegate() = apiDelegateMutex.withLock {
        _apiDelegate ?: createApiDelegateIfTokenAvailable() ?: createApiDelegateWithNewToken()
    }

    private suspend fun createApiDelegateIfTokenAvailable(): SpotifyClientApi? {
        val savedAccessToken = sessionRepository.getSpotifyAuthToken()
        if (savedAccessToken != null && !sessionRepository.isSpotifyAuthExpired()) {
            _apiDelegate = createApiDelegate(savedAccessToken)
        }
        return _apiDelegate
    }

    private suspend fun createApiDelegateWithNewToken(): SpotifyClientApi {
        return when (val spotifyAuthResult = spotifyAuthDispatcher.requestAuthorization()) {
            is SpotifyAuthResult.Success -> createApiDelegate(spotifyAuthResult.accessToken.token).also { _apiDelegate = it }
            is SpotifyAuthResult.Failure -> throw AuthenticationException("Failed to create API delegate due to auth failure: ${spotifyAuthResult.reason}")
        }
    }

    private suspend fun createApiDelegate(accessToken: String): SpotifyClientApi {
        val apiAuthorization = SpotifyUserAuthorization(tokenString = accessToken)
        val apiOptions = SpotifyApiOptions(automaticRefresh = false, allowBulkRequests = true, testTokenValidity = false)
        return SpotifyClientApiBuilder(authorization = apiAuthorization, options = apiOptions).build()
    }

    suspend fun fetchPlayHistory(): List<PlayHistory> = doSafeApiCall {
        Timber.v("Fetching recently played tracks")
        getApiDelegate().player.getRecentlyPlayed(API_ITEM_LIMIT_LOW).items
    }

    suspend fun fetchTopTracks(timeRange: ClientPersonalizationApi.TimeRange): List<Track> = doSafeApiCall {
        Timber.v("Fetching top tracks -- ${timeRange.id}")
        getApiDelegate().personalization
            .getTopTracks(API_ITEM_LIMIT_LOW_MAX_PAGING, timeRange = timeRange)
            .getAllItems().filterNotNull()
    }

    suspend fun fetchAllSavedAlbums(): List<SavedAlbum> = doSafeApiCall {
        Timber.v("Fetching saved albums")
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

    suspend fun <T> apiCall(
        callDescription: String,
        allowRetries: Boolean = true,
        call: suspend SpotifyClientApi.() -> T?
    ): T? = withContext(Dispatchers.IO) {
        try {
            Timber.i("Calling Spotify API - $callDescription")
            getApiDelegate().call()
        } catch (e: Exception) {
            when {
                e.isAuthError() && allowRetries -> {
                    Timber.w(e, "Spotify auth failure, refreshing auth and retrying API call - $callDescription")
                    if (refreshAuthToken()) {
                        apiCall(callDescription, allowRetries = false, call)
                    } else {
                        null
                    }
                }
                e.isExpectedError() -> {
                    Timber.e(e, "Spotify API call failed - $callDescription")
                    null
                }
                else -> throw e
            }
        }
    }

    private fun Throwable.isAuthError() = this is AuthenticationException || this is ReAuthenticationNeededException
            || this is BadRequestException && statusCode == 401

    private fun Throwable.isExpectedError() = this is SpotifyException || this is SocketTimeoutException
            || this is ConnectTimeoutException || this is UnknownHostException

    // TODO: remove num503s param when Spotify's issue with frequent 503s is resolved
    private suspend fun <T> doSafeApiCall(num503s: Int = 0, apiCall: suspend () -> T): T = withContext(Dispatchers.IO) {
        wrapResult {
            apiCall()
        }.handleAny(AuthenticationException::class, ReAuthenticationNeededException::class) {
            refreshAuthTokenUnsafe()
            apiCall()
        }.handle(BadRequestException::class) { e ->
            when (e.statusCode) {
                401 -> {
                    refreshAuthTokenUnsafe()
                    apiCall()
                }
                in 500..599 -> {
                    Timber.w(e, "API call failed due to server error ${e.statusCode}, trying one more time...")
                    apiCall()
                }
                else -> throw e
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

    private suspend fun refreshAuthToken(): Boolean {
        val spotifyAuthResult = spotifyAuthDispatcher.requestAuthorization()
        if (spotifyAuthResult is SpotifyAuthResult.Success) {
            getApiDelegate().updateToken {
                accessToken = spotifyAuthResult.accessToken.token
                expiresIn = spotifyAuthResult.accessToken.expiresIn // duration of validity in seconds
            }
        }
        return spotifyAuthResult is SpotifyAuthResult.Success
    }

    private suspend fun refreshAuthTokenUnsafe() {
        when (val spotifyAuthResult = spotifyAuthDispatcher.requestAuthorization()) {
            is SpotifyAuthResult.Success -> getApiDelegate().updateToken {
                accessToken = spotifyAuthResult.accessToken.token
                expiresIn = spotifyAuthResult.accessToken.expiresIn // duration of validity in seconds
            }
            is SpotifyAuthResult.Failure -> throw ReAuthenticationNeededException(message = "failed to refresh auth")
        }
    }
}
