package io.libzy.ui.findalbum.results

import io.libzy.domain.RecommendationCategory

/**
 * @property resultsRating A rating from 1-5 stars of how accurate the album results are to the user's current mood
 */
data class ResultsUiState(
    val loading: Boolean,
    val recommendationCategories: List<RecommendationCategory> = emptyList(),
    val resultsRating: Int? = null
)
