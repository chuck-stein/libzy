package io.libzy.spotify.remote

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import io.libzy.R
import timber.log.Timber
import javax.inject.Inject

class SpotifyAppRemoteService @Inject constructor(private val context: Context) {

    private val connectionParams =
        ConnectionParams.Builder(context.getString(R.string.spotify_client_id))
            .setRedirectUri(context.getString(R.string.spotify_auth_redirect_uri))
            .build()

    private var appRemote: SpotifyAppRemote? = null

    private var remoteInUse = false

    fun connect(onFailure: () -> Unit) {
        remoteInUse = true
        SpotifyAppRemote.disconnect(appRemote)
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {

            override fun onConnected(remote: SpotifyAppRemote) {
                if (remoteInUse) {
                    appRemote = remote
                    // TODO: ensure subscription isn't garbage collected when this function loses scope
                    // TODO: handle subscription errors/lifecycle in UI
                    remote.playerApi.subscribeToPlayerState().setEventCallback {
                        // TODO: update a StateFlow<PlayerState> if we ever wish to react to player context
                        Timber.d("Spotify switched player state: $it")
                    }
                    remote.playerApi.subscribeToPlayerContext().setEventCallback {
                        // TODO: update a StateFlow<PlayerContext> if we ever wish to react to player context
                        Timber.d("Spotify switched player context: $it")
                    }
                }
            }

            override fun onFailure(exception: Throwable) {
                Timber.e(exception, "Failed to connect Spotify app remote!")
                onFailure()
            }

        })
    }

    fun disconnect() {
        remoteInUse = false
        SpotifyAppRemote.disconnect(appRemote)
    }

    fun playAlbum(spotifyUri: String) {
        requireRemote().playerApi.setShuffle(false)
        requireRemote().playerApi.play(spotifyUri)
    }

    private fun requireRemote(): SpotifyAppRemote {
        appRemote.let { remote ->
            if (remote?.isConnected == true) return remote
            else throw IllegalStateException("Spotify app remote not connected!")
        }
    }

}
