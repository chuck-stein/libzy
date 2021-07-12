package io.libzy.ui.results

import io.libzy.model.AlbumResult

/**
 * @property resultsRating A rating from 1-5 stars of how accurate the album results are to the user's current mood
 */
data class ResultsUiState(
    val albumResults: List<AlbumResult>,
    val resultsRating: Int? = null
)

enum class ResultsUiEvent {
    SPOTIFY_REMOTE_FAILURE
}
