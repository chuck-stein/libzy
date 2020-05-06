package com.chuckstein.libzy.auth

import kotlinx.coroutines.channels.Channel

/*
TODO:
    - make this an injected dependency instead of a singleton
    - is this a valid usage of the Mediator pattern?
    - read up on coroutines to do this in the best way
    - with every coroutine I've made, think about timing out, exceptions, cancelling, etc, and manage the job
 */
object SpotifyAuthMediator {

    var authServerProxy: SpotifyAuthServerProxy? = null
        set(value) {
            field = value
            if (value != null) authServerProxyChannel.offer(value)
        }

    // TODO: are channels the best way to do what I'm doing? it's only emitting one value...
    private val authServerProxyChannel = Channel<SpotifyAuthServerProxy>()

    suspend fun refreshAccessToken() =
        authServerProxy?.refreshAccessToken() ?: awaitAuthServerProxy().refreshAccessToken()

    suspend fun connectSpotify() {
        authServerProxy?.connectSpotify() ?: awaitAuthServerProxy().connectSpotify()
    }

    private suspend fun awaitAuthServerProxy() = authServerProxyChannel.receive()

}