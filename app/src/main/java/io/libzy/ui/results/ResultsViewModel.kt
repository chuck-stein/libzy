package io.libzy.ui.results

import androidx.lifecycle.viewModelScope
import io.libzy.BuildConfig
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.Query
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.remote.SpotifyAppRemoteService
import io.libzy.ui.common.ScreenViewModel
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
) : ScreenViewModel<ResultsUiState, ResultsUiEvent>() {

    override val initialUiState = ResultsUiState(loading = true)

    private var albumRecommendationJob: Job? = null

    fun recommendAlbums(query: Query) {
        albumRecommendationJob?.cancel()
        albumRecommendationJob = viewModelScope.launch {
            userLibraryRepository.albums.collect { albums ->
                updateUiState {
                    copy(albumResults = recommendationService.recommendAlbums(query, albums), loading = false)
                }
                analyticsDispatcher.sendSubmitQueryEvent(query, uiState.value.albumResults)
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
        }
    }

    fun connectSpotifyAppRemote() {
        spotifyAppRemoteService.connect {
            // TODO: handle connection failure: https://chilipot.atlassian.net/browse/LIB-253
        }
    }

    fun disconnectSpotifyAppRemote() {
        spotifyAppRemoteService.disconnect()
    }

    fun playAlbum(spotifyUri: String) {
        analyticsDispatcher.sendPlayAlbumEvent(spotifyUri)

        if (BuildConfig.DEBUG) logAlbumDetails(spotifyUri)

        try {
            spotifyAppRemoteService.playAlbum(spotifyUri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to play album remotely")
            produceUiEvent(ResultsUiEvent.SPOTIFY_REMOTE_FAILURE)
        }
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

    companion object {
        // TODO: determine this based on screen size instead of hardcoding it
        private const val NUM_PLACEHOLDER_RESULTS = 50
    }
}
