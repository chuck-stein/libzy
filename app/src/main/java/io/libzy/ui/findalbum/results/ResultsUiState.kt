package io.libzy.ui.findalbum.results

import io.libzy.domain.RecommendationCategory


sealed interface ResultsUiState {

    object Loading : ResultsUiState

    /**
     * @property recommendationCategories The album results, grouped into categories
     * @property resultsRating A rating from 1-5 stars of how accurate the album results are to the user's current mood
     */
    data class Loaded(
        val recommendationCategories: List<RecommendationCategory>,
        val resultsRating: Int? = null
    ) : ResultsUiState
}
