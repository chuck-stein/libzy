package io.libzy.ui.findalbum.results

import io.libzy.domain.RecommendationCategory


sealed interface ResultsUiState {

    object Loading : ResultsUiState

    /**
     * @property recommendationCategories The album results, grouped into categories
     * @property submittingFeedback Whether the user is currently submit
     */
    data class Loaded(
        val recommendationCategories: List<RecommendationCategory>,
        val submittingFeedback: Boolean = false,
        val currentAlbumUri: String? = null,
    ) : ResultsUiState
}
