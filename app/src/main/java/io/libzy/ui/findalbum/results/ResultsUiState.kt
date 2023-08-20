package io.libzy.ui.findalbum.results

import io.libzy.ui.common.component.AlbumUiState
import io.libzy.util.TextResource

sealed interface ResultsUiState {

    object Loading : ResultsUiState

    /**
     * @property recommendationCategories The album results, grouped into categories
     * @property submittingFeedback Whether the user is currently submitting feedback on the recommended albums
     * @property currentAlbumUri The Spotify URI of the album currently playing, if any
     */
    data class Loaded(
        val recommendationCategories: List<RecommendationCategoryUiState>,
        val submittingFeedback: Boolean = false,
        val currentAlbumUri: String? = null
    ) : ResultsUiState
}

data class RecommendationCategoryUiState(
    val title: TextResource,
    val albums: List<AlbumUiState>
)