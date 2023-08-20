package io.libzy.ui.findalbum.results

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import io.libzy.BuildConfig
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.Query
import io.libzy.domain.RecommendationCategory
import io.libzy.recommendation.ListeningRecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.remote.SpotifyAppRemoteService
import io.libzy.ui.common.LibzyViewModel
import io.libzy.ui.common.component.toUiState
import io.libzy.util.TextResource
import io.libzy.util.capitalizeAllWords
import io.libzy.util.emptyTextResource
import io.libzy.util.joinToUserFriendlyString
import io.libzy.util.toTextResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val listeningRecommendationService: ListeningRecommendationService,
    private val spotifyAppRemoteService: SpotifyAppRemoteService,
    private val analyticsDispatcher: AnalyticsDispatcher
) : LibzyViewModel<ResultsUiState, ResultsUiEvent>() {

    override val initialUiState = ResultsUiState.Loading

    private var albumRecommendationJob: Job? = null

    fun recommendAlbums(query: Query, resources: Resources) {
        albumRecommendationJob?.cancel()
        albumRecommendationJob = viewModelScope.launch {
            userLibraryRepository.albums.collect { albums ->
                val recommendationCategories = listeningRecommendationService.recommendAlbums(query, albums)
                    .map { it.toUiState() }
                updateUiState {
                    ResultsUiState.Loaded(recommendationCategories)
                }
                analyticsDispatcher.sendViewAlbumResultsEvent(query, recommendationCategories, resources)
            }
        }
    }

    fun openRateResultsDialog() {
        updateUiState<ResultsUiState.Loaded> {
            copy(submittingFeedback = true)
        }
    }

    fun dismissRateResultsDialog() {
        updateUiState<ResultsUiState.Loaded> {
            copy(submittingFeedback = false)
        }
    }

    fun rateResults(rating: Int, feedback: String?) {
        analyticsDispatcher.sendRateAlbumResultsEvent(rating, feedback)
        dismissRateResultsDialog()
    }

    fun connectSpotifyAppRemote() {
        spotifyAppRemoteService.connect()
    }

    fun disconnectSpotifyAppRemote() {
        spotifyAppRemoteService.disconnect()
    }

    fun playAlbum(spotifyUri: String) {
        if (BuildConfig.DEBUG) logAlbumDetails(spotifyUri)

        spotifyAppRemoteService.playAlbum(spotifyUri, onFailure = {
            Timber.e("Failed to play album remotely")
            produceUiEvent(ResultsUiEvent.SPOTIFY_REMOTE_FAILURE)
        })

        updateUiState<ResultsUiState.Loaded> {
            copy(currentAlbumUri = spotifyUri)
        }

        viewModelScope.launch {
            val album = userLibraryRepository.getAlbumFromUri(spotifyUri)
            analyticsDispatcher.sendPlayAlbumEvent(album)
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

    private fun RecommendationCategory.toUiState() = RecommendationCategoryUiState(
        albums = albums.map { it.toUiState() },
        title = when (relevance) {
            is RecommendationCategory.Relevance.Full -> R.string.full_match_category_title.toTextResource()
            is RecommendationCategory.Relevance.Partial -> {
                val adjectiveFormatString = relevance.adjectives.indices.map { "%$it" }.joinToUserFriendlyString()
                val adjectiveFormatArgs = relevance.adjectives.map { it.toTextResource() }

                val capitalizedGenre = relevance.genre?.capitalizeAllWords()

                val nounFormatArg: TextResource = when (relevance.familiarity) {
                    Query.Familiarity.CURRENT_FAVORITE -> when {
                        capitalizedGenre != null -> TextResource.Id(R.string.current_genre_favorites, capitalizedGenre)
                        else -> TextResource.Id(R.string.current_favorites)
                    }
                    Query.Familiarity.RELIABLE_CLASSIC -> when {
                        capitalizedGenre != null -> TextResource.Id(R.string.reliable_genre_classics, capitalizedGenre)
                        else -> TextResource.Id(R.string.reliable_classics)
                    }
                    Query.Familiarity.UNDERAPPRECIATED_GEM -> when {
                        capitalizedGenre != null -> TextResource.Id(R.string.underappreciated_genre, capitalizedGenre)
                        else -> TextResource.Id(R.string.underappreciated_gems)
                    }
                    null -> capitalizedGenre?.toTextResource() ?: emptyTextResource
                }
                val nounFormatString = "%${adjectiveFormatArgs.lastIndex + 1}"

                TextResource.Composite(
                    formatArgs = adjectiveFormatArgs + nounFormatArg,
                    formattedText = buildString {
                        append(adjectiveFormatString)
                        if (adjectiveFormatString.isNotEmpty() && nounFormatString.isNotEmpty()) {
                            append(" ")
                        }
                        append(nounFormatString)
                    }
                )
            }
        }
    )
}
