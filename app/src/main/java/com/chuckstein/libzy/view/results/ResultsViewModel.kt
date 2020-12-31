package com.chuckstein.libzy.view.results

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.chuckstein.libzy.model.AlbumResult
import com.chuckstein.libzy.model.Query
import com.chuckstein.libzy.recommendation.RecommendationService
import com.chuckstein.libzy.repository.UserLibraryRepository
import com.chuckstein.libzy.spotify.remote.SpotifyAppRemoteService
import javax.inject.Inject

class ResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val spotifyAppRemoteService: SpotifyAppRemoteService
) : ViewModel() {

    companion object {
        private val TAG = ResultsViewModel::class.java.simpleName
    }

    // TODO: delete if unused
    val spotifyPlayerState = spotifyAppRemoteService.playerState
    val spotifyPlayerContext = spotifyAppRemoteService.playerContext

    fun getResults(query: Query): LiveData<List<AlbumResult>> =
        recommendationService.recommendAlbums(userLibraryRepository.libraryAlbums, query)
    
    fun connectSpotifyAppRemote(onFailure: () -> Unit) {
        spotifyAppRemoteService.connect(onFailure)
    }

    fun disconnectSpotifyAppRemote() {
        spotifyAppRemoteService.disconnect()
    }

    fun playAlbum(spotifyUri: String) {
        spotifyAppRemoteService.playAlbum(spotifyUri)
    }
}