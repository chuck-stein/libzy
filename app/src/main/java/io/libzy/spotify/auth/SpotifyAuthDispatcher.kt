package io.libzy.spotify.auth

import android.net.ConnectivityManager
import com.spotify.sdk.android.auth.AuthorizationRequest
import io.libzy.util.connectedToNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*
TODO:
    - look at actual request dispatchers like Spring MVC's for design advice
    - read up on coroutines to do this in the best way
    - unit test this
 */
@Singleton
class SpotifyAuthDispatcher @Inject constructor(private val connectivityManager: ConnectivityManager) {

    var authClientProxy: SpotifyAuthClientProxy? = null
        set(proxy) {
            field = proxy
            if (proxy == null && pendingAuthCallbacks.isNotEmpty()) {
                requestsWaitingForProxy = true
            } else if (proxy != null && requestsWaitingForProxy) {
                requestsWaitingForProxy = false
                proxy.initiateSpotifyAuthRequest(onAuthComplete)
            }
        }

    private var requestsWaitingForProxy = false

    private val pendingAuthCallbacks = mutableListOf<SpotifyAuthCallback>()

    private val onAuthComplete = object : SpotifyAuthCallback {
        override fun onSuccess(accessToken: SpotifyAccessToken) {
            for (pendingAuthCallback in pendingAuthCallbacks) pendingAuthCallback.onSuccess(accessToken)
            pendingAuthCallbacks.clear()
        }

        override fun onFailure(reason: String?) {
            for (pendingAuthCallback in pendingAuthCallbacks) pendingAuthCallback.onFailure(reason)
            pendingAuthCallbacks.clear()
        }
    }

    suspend fun requestAuthorization(
        withTimeout: Boolean = true,
        authOptions: AuthorizationRequest.Builder.() -> AuthorizationRequest.Builder = { this },
    ): SpotifyAuthResult = withContext(Dispatchers.IO) {
        if (!connectivityManager.connectedToNetwork()) {
            SpotifyAuthResult.Failure("No network connection")
        } else {
            val timeout = if (withTimeout) authTimeout else Duration.INFINITE
            withTimeoutOrNull(timeout) {
                suspendCancellableCoroutine<SpotifyAuthResult> { continuation ->
                    // TODO: Reassess which dispatchers we need here.
                    //  It's a network operation so we open with Dispatchers.IO, but spotify auth SDK
                    //  must be invoked from the main thread, so we launch on the main thread here.
                    //  Is Dispatchers.IO necessary? Instead of launching on Main can we do withContext(Main) here?
                    CoroutineScope(Dispatchers.Main).launch {

                        // initialize an auth callback to unsuspend the coroutine upon completion with either a token or exception
                        val spotifyAuthCallback = object : SpotifyAuthCallback {
                            override fun onSuccess(accessToken: SpotifyAccessToken) {
                                if (continuation.isActive) continuation.resume(SpotifyAuthResult.Success(accessToken))
                            }

                            override fun onFailure(reason: String?) {
                                if (continuation.isActive) continuation.resume(SpotifyAuthResult.Failure(reason))
                            }
                        }

                        // add auth callback to the list of pending callbacks so it will be called when auth completes
                        pendingAuthCallbacks.add(spotifyAuthCallback)

                        // remove auth callback upon coroutine cancellation, so it is not called with nowhere to continue
                        continuation.invokeOnCancellation { pendingAuthCallbacks.remove(spotifyAuthCallback) }

                        // if this is the only pending auth callback, initiate auth since it is not already in progress
                        if (pendingAuthCallbacks.size == 1) {
                            authClientProxy.let {
                                if (it == null) requestsWaitingForProxy = true
                                else it.initiateSpotifyAuthRequest(onAuthComplete, authOptions)
                            }
                        }
                    }
                }
            } ?: SpotifyAuthResult.Failure("Timed out while waiting for Spotify auth callback")
        }
    }.also { result ->
        if (result is SpotifyAuthResult.Failure) {
            Timber.e("Failed to authorize Spotify: ${result.reason}")
        }
    }
}

private val authTimeout = 20.seconds

sealed interface SpotifyAuthResult {
    data class Success(val accessToken: SpotifyAccessToken) : SpotifyAuthResult
    data class Failure(val reason: String?) : SpotifyAuthResult
}