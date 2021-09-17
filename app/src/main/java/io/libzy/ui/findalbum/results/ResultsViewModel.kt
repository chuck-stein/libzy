package io.libzy.ui.findalbum.results

import androidx.lifecycle.viewModelScope
import io.libzy.BuildConfig
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.Query
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.remote.SpotifyAppRemoteService
import io.libzy.ui.common.LibzyViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val spotifyAppRemoteService: SpotifyAppRemoteService,
    private val analyticsDispatcher: AnalyticsDispatcher
) : LibzyViewModel<ResultsUiState, ResultsUiEvent>() {

    override val initialUiState = ResultsUiState(loading = true)

    private var albumRecommendationJob: Job? = null

    fun recommendAlbums(query: Query) {
        albumRecommendationJob?.cancel()
        albumRecommendationJob = viewModelScope.launch {
            userLibraryRepository.albums.collect { albums ->
                updateUiState {
                    copy(recommendationCategories = recommendationService.recommendAlbums(query, albums), loading = false)
                }
                // TODO: create new corresponding analytics event for receiving recommendation categories
//                analyticsDispatcher.sendViewAlbumResultsEvent(query, uiState.value.albumResults)
            }
        }
    }

    fun rateResults(rating: Int) {
        updateUiState {
            copy(resultsRating = rating)
        }
    }

    fun sendResultsRating() {
        uiState.value.resultsRating?.let {
            analyticsDispatcher.sendRateAlbumResultsEvent(it)
            updateUiState {
                copy(resultsRating = null) // reset the rating so that it is not sent again later unless changed
            }
        }
    }

    fun connectSpotifyAppRemote() {
        spotifyAppRemoteService.connect()
    }

    fun disconnectSpotifyAppRemote() {
        spotifyAppRemoteService.disconnect()
    }

    fun playAlbum(spotifyUri: String) {
        analyticsDispatcher.sendPlayAlbumEvent(spotifyUri)

        if (BuildConfig.DEBUG) logAlbumDetails(spotifyUri)

        spotifyAppRemoteService.playAlbum(spotifyUri, onFailure = {
            Timber.e("Failed to play album remotely")
            produceUiEvent(ResultsUiEvent.SPOTIFY_REMOTE_FAILURE)
        })
    }

    private fun logAlbumDetails(spotifyUri: String) {
        viewModelScope.launch {
            val album = userLibraryRepository.getAlbumFromUri(spotifyUri)
            val albumDetails =
                """
                title: ${album.title}
                genres: ${album.genres}
                acousticness: ${album.audioFeatures.acousticness}
                danceability: ${album.audioFeatures.danceability}
                energy: ${album.audioFeatures.energy}
                instrumentalness: ${album.audioFeatures.instrumentalness}
                valence: ${album.audioFeatures.valence}
                longTermFavorite: ${album.familiarity.longTermFavorite}
                mediumTermFavorite: ${album.familiarity.mediumTermFavorite}
                shortTermFavorite: ${album.familiarity.shortTermFavorite}
                recentlyPlayed: ${album.familiarity.recentlyPlayed}
                lowFamiliarity: ${album.familiarity.isLowFamiliarity()}
                """
            Timber.d(albumDetails.trimIndent())
        }
    }
}
