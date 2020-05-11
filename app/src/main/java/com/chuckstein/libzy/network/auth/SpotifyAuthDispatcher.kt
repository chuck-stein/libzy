package com.chuckstein.libzy.network.auth

import android.os.Handler
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
TODO:
    - make this an injected dependency instead of a singleton
    - look at actual request dispatchers like Spring MVC's for design advice
    - read up on coroutines to do this in the best way
    - unit test this
 */
object SpotifyAuthDispatcher {

    private const val AUTH_TIMEOUT_MILLIS = 10_000L
    private val TAG = SpotifyAuthDispatcher::class.java.simpleName

    var authClientProxy: SpotifyAuthClientProxy? = null
        set(proxy) {
            field = proxy
            if (proxy != null && requestsWaitingForProxy) proxy.initiateAuthRequest(onAuthComplete)
        }

    private var requestsWaitingForProxy = false

    private val pendingAuthCallbacks = mutableListOf<SpotifyAuthCallback>()

    private val onAuthComplete = object: SpotifyAuthCallback {
        override fun onSuccess(accessToken: String) {
            for (pendingAuthCallback in pendingAuthCallbacks) {
                pendingAuthCallback.onSuccess(accessToken)
            }
            pendingAuthCallbacks.clear()
        }

        override fun onFailure(exception: SpotifyAuthException) {
            for (pendingAuthCallback in pendingAuthCallbacks) {
                pendingAuthCallback.onFailure(exception)
            }
            pendingAuthCallbacks.clear()
        }

    }

    suspend fun requestAuthorization(): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<String> { continuation ->
            CoroutineScope(Dispatchers.Main).launch { // TODO: is this the best way to ensure suspendCoroutine block runs on main thread?

                // initialize an auth callback to unsuspend the coroutine upon completion with either a token or exception
                val spotifyAuthCallback = object : SpotifyAuthCallback {
                    override fun onSuccess(accessToken: String) {
                        continuation.resume(accessToken)
                    }

                    override fun onFailure(exception: SpotifyAuthException) {
                        continuation.resumeWithException(exception)
                    }
                }

                // add auth callback to the list of pending callbacks so it will be called when auth completes
                pendingAuthCallbacks.add(spotifyAuthCallback)

                // create a timeout to fail with an exception if auth client doesn't return in a reasonable time frame
                Handler().postDelayed({ // TODO: use withTimeout() instead?
                    if (continuation.isActive) {
                        pendingAuthCallbacks.remove(spotifyAuthCallback)
                        val errorMessage = "Timed out while waiting for Spotify auth callback"
                        Log.e(TAG, errorMessage)
                        continuation.resumeWithException(SpotifyAuthException(errorMessage))
                    }
                }, AUTH_TIMEOUT_MILLIS)

                // remove auth callback upon coroutine cancellation, so it is not called with nowhere to continue
                continuation.invokeOnCancellation {
                    pendingAuthCallbacks.remove(spotifyAuthCallback)
                }

                // if this is the only pending auth callback, initiate auth since it is not already in progress
                if (pendingAuthCallbacks.size == 1) {
                    authClientProxy.let {
                        if (it == null) requestsWaitingForProxy = true
                        else it.initiateAuthRequest(onAuthComplete)
                    }
                }
            }
        }
    }
}