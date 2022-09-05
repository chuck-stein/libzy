package io.libzy.ui.findalbum.results

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import io.libzy.BuildConfig
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.Query
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.remote.SpotifyAppRemoteService
import io.libzy.ui.common.LibzyViewModel
import io.libzy.util.androidAppUriFor
import io.libzy.util.isPackageInstalled
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val spotifyAppRemoteService: SpotifyAppRemoteService,
    private val analyticsDispatcher: AnalyticsDispatcher
) : LibzyViewModel<ResultsUiState, ResultsUiEvent>() {

    override val initialUiState = ResultsUiState.Loading

    private var albumRecommendationJob: Job? = null

    fun recommendAlbums(query: Query) {
        albumRecommendationJob?.cancel()
        albumRecommendationJob = viewModelScope.launch {
            userLibraryRepository.albums.collect { albums ->
                updateUiState {
                    ResultsUiState.Loaded(recommendationService.recommendAlbums(query, albums))
                }
                // TODO: create new corresponding analytics event for receiving recommendation categories
//                analyticsDispatcher.sendViewAlbumResultsEvent(query, uiState.value.albumResults)
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
        analyticsDispatcher.sendPlayAlbumEvent(spotifyUri)

        if (BuildConfig.DEBUG) logAlbumDetails(spotifyUri)

        spotifyAppRemoteService.playAlbum(spotifyUri, onFailure = {
            Timber.e("Failed to play album remotely")
            produceUiEvent(ResultsUiEvent.SPOTIFY_REMOTE_FAILURE)
        })

        updateUiState<ResultsUiState.Loaded> {
            copy(currentAlbumUri = spotifyUri)
        }
    }

    fun openSpotify(context: Context) { // not leaking context here because we don't store it in memory
        val spotifyIsInstalled = context.packageManager.isPackageInstalled(SPOTIFY_PACKAGE_NAME)
        val currentAlbumUri = (uiState.value as? ResultsUiState.Loaded)?.currentAlbumUri

        val uri = when {
            !spotifyIsInstalled -> Uri.parse(PLAY_STORE_URI).withSpotifyPlayStoreId()
            currentAlbumUri != null -> Uri.parse(currentAlbumUri)
            else -> androidAppUriFor(SPOTIFY_PACKAGE_NAME)
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            putExtra(Intent.EXTRA_REFERRER, androidAppUriFor(context.packageName))
        }
        ContextCompat.startActivity(context, intent, null)
    }

    private fun Uri.withSpotifyPlayStoreId() = buildUpon()
        .appendQueryParameter(PLAY_STORE_ID_QUERY_PARAM, SPOTIFY_PACKAGE_NAME)
        .build()

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

private const val SPOTIFY_PACKAGE_NAME = "com.spotify.music"
private const val PLAY_STORE_URI = "https://play.google.com/store/apps/details"
private const val PLAY_STORE_ID_QUERY_PARAM = "id"
