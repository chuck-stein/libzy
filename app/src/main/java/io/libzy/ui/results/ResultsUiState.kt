package io.libzy.ui.results

import io.libzy.domain.AlbumResult

/**
 * @property resultsRating A rating from 1-5 stars of how accurate the album results are to the user's current mood
 */
data class ResultsUiState(
    val loading: Boolean,
    val albumResults: List<AlbumResult> = emptyList(),
    val resultsRating: Int? = null
)
