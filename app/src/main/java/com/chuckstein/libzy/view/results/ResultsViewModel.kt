package com.chuckstein.libzy.view.results

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.chuckstein.libzy.BuildConfig
import com.chuckstein.libzy.model.Query
import com.chuckstein.libzy.recommendation.RecommendationService
import com.chuckstein.libzy.repository.UserLibraryRepository
import com.chuckstein.libzy.spotify.remote.SpotifyAppRemoteService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val spotifyAppRemoteService: SpotifyAppRemoteService
) : ViewModel() {

    companion object {
        private val TAG = ResultsViewModel::class.java.simpleName
    }

    // TODO: use a shared view model between QueryFragment and ResultsFragment so we can have access to `query` here
    //  and not need the getResults() function
//    val albumResults = userLibraryRepository.libraryAlbums.map {
//        recommendationService.recommendAlbums(it, query)
//    }

    // TODO: delete if unused
    val spotifyPlayerState = spotifyAppRemoteService.playerState
    val spotifyPlayerContext = spotifyAppRemoteService.playerContext

    fun getResults(query: Query) = userLibraryRepository.libraryAlbums.map {
        recommendationService.recommendAlbums(it, query)
    }

    fun connectSpotifyAppRemote(onFailure: () -> Unit) {
        spotifyAppRemoteService.connect(onFailure)
    }

    fun disconnectSpotifyAppRemote() {
        spotifyAppRemoteService.disconnect()
    }

    fun playAlbum(spotifyUri: String) {
        if (BuildConfig.DEBUG) logAlbumDetails(spotifyUri)
        spotifyAppRemoteService.playAlbum(spotifyUri)
    }

    private fun logAlbumDetails(spotifyUri: String) {
        GlobalScope.launch {
            val album = userLibraryRepository.getAlbumFromUri(spotifyUri)
            Log.d(
                TAG, "title: ${album.title}\ngenres: ${album.genres}\n" +
                        "acousticness: ${album.audioFeatures.acousticness}\n" +
                        "danceability: ${album.audioFeatures.danceability}\n" +
                        "energy: ${album.audioFeatures.energy}\n" +
                        "instrumentalness: ${album.audioFeatures.instrumentalness}\n" +
                        "valence: ${album.audioFeatures.valence}\n" +
                        "longTermFavorite: ${album.familiarity.longTermFavorite}\n" +
                        "mediumTermFavorite: ${album.familiarity.mediumTermFavorite}\n" +
                        "shortTermFavorite: ${album.familiarity.shortTermFavorite}\n" +
                        "recentlyPlayed: ${album.familiarity.recentlyPlayed}\n" +
                        "lowFamiliarity: ${album.familiarity.isLowFamiliarity()}\n"
            )
        }
    }
}