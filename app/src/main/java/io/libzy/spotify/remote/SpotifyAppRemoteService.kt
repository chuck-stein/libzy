package io.libzy.spotify.remote

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import io.libzy.R
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject

// TODO: test extensively -- lots of edge cases: https://chilipot.atlassian.net/browse/LIB-275
class SpotifyAppRemoteService @Inject constructor(private val context: Context) {

    sealed interface RemoteState {
        object Inactive : RemoteState
        data class Connecting(val onFailure: () -> Unit = {}, val pendingSpotifyUri: String? = null) : RemoteState
        data class Connected(val appRemote: SpotifyAppRemote) : RemoteState
        data class ConnectionFailed(val failure: Throwable) : RemoteState
    }

    private val remoteState = MutableStateFlow<RemoteState>(RemoteState.Inactive)

    // TODO: get these values from ApiKeys class: https://chilipot.atlassian.net/browse/LIB-278
    private val connectionParams =
        ConnectionParams.Builder(context.getString(R.string.spotify_client_id))
            .setRedirectUri(context.getString(R.string.spotify_auth_redirect_uri))
            .build()

    fun connect(onFailure: () -> Unit = {}, pendingSpotifyUri: String? = null) {
        if (remoteState.value is RemoteState.Connecting) return
        (remoteState.value as? RemoteState.Connected)?.let { SpotifyAppRemote.disconnect(it.appRemote) }
        remoteState.value = RemoteState.Connecting(onFailure, pendingSpotifyUri)

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {

            override fun onConnected(remote: SpotifyAppRemote) {
                (remoteState.value as? RemoteState.Connecting)?.let { connectingRemoteState ->
                    remoteState.value = RemoteState.Connected(remote)
                    connectingRemoteState.pendingSpotifyUri?.let { playAlbum(it, connectingRemoteState.onFailure) }
                }
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "Failed to connect Spotify app remote!")
                (remoteState.value as? RemoteState.Connecting)?.let {
                    it.onFailure()
                    remoteState.value = RemoteState.ConnectionFailed(failure)
                }
            }
        })
    }

    fun disconnect() {
        (remoteState.value as? RemoteState.Connected)?.let {
            SpotifyAppRemote.disconnect(it.appRemote)
        }
        remoteState.value = RemoteState.Inactive
    }

    fun playAlbum(spotifyUri: String, onFailure: () -> Unit) {
        remoteState.value.let {
            when (it) {
                is RemoteState.Inactive -> {
                    // no-op
                }
                is RemoteState.Connecting -> {
                    remoteState.value = RemoteState.Connecting(onFailure, pendingSpotifyUri = spotifyUri)
                }
                is RemoteState.ConnectionFailed -> {
                    connect(onFailure, pendingSpotifyUri = spotifyUri)
                }
                is RemoteState.Connected -> {
                    with(it.appRemote) {
                        if (!isConnected) {
                            Timber.e("Spotify app remote is not connected when it should be!")
                            onFailure()
                        }
                        playerApi.setShuffle(false)
                        playerApi.play(spotifyUri).setErrorCallback { err ->
                            Timber.e(err, "Spotify app remote's 'play' operation failed!")
                            onFailure()
                        }
                    }
                }
            }
        }
    }
}
