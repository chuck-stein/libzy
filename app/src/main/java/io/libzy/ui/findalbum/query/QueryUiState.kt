package io.libzy.ui.findalbum.query

import io.libzy.R
import io.libzy.domain.Query

data class QueryUiState(
    val stepOrder: List<Query.Parameter>,
    val currentStep: QueryStep,
    val previousStepIndex: Int? = null,
    val query: Query = Query()
) {
    val currentStepIndex = stepOrder.indexOfFirst { it == currentStep.parameterType }
    private val onFinalStep = currentStepIndex == stepOrder.size - 1
    val pastFirstStep = currentStepIndex > 0
    val continueButtonText = if (onFinalStep) R.string.ready_button else R.string.continue_button
    val continueButtonEnabled = when (currentStep) {
        is QueryStep.Familiarity -> query.familiarity != null
        is QueryStep.Instrumentalness -> query.instrumental != null
        is QueryStep.Acousticness -> query.acousticness != null
        is QueryStep.Valence -> query.valence != null
        is QueryStep.Energy -> query.energy != null
        is QueryStep.Danceability -> query.danceability != null
        is QueryStep.Genres -> query.genres != null
    }
    val startOverButtonVisible = currentStep !is QueryStep.Genres.Search && pastFirstStep
    val navigatingForward = previousStepIndex == null || previousStepIndex <= currentStepIndex

    init {
        require(currentStepIndex in stepOrder.indices && previousStepIndex?.let { it in stepOrder.indices } ?: true) {
            "currentStep and previousStepIndex must be contained within stepOrder " +
                    "(currentStep = $currentStep, previousStepIndex = $previousStepIndex, stepOrder = $stepOrder)"
        }
    }

    companion object {
        val DEFAULT_STEP_ORDER = listOf(
            Query.Parameter.FAMILIARITY,
            Query.Parameter.INSTRUMENTALNESS,
            Query.Parameter.ACOUSTICNESS,
            Query.Parameter.VALENCE,
            Query.Parameter.ENERGY,
            Query.Parameter.DANCEABILITY,
            Query.Parameter.GENRES
        )
    }
}

sealed class QueryStep(val parameterType: Query.Parameter) {

    object Familiarity : QueryStep(Query.Parameter.FAMILIARITY)

    object Instrumentalness : QueryStep(Query.Parameter.INSTRUMENTALNESS)

    object Acousticness : QueryStep(Query.Parameter.ACOUSTICNESS)

    object Valence : QueryStep(Query.Parameter.VALENCE)

    object Energy : QueryStep(Query.Parameter.ENERGY)

    object Danceability : QueryStep(Query.Parameter.DANCEABILITY)

    sealed class Genres : QueryStep(Query.Parameter.GENRES) {
        abstract val genreOptions: List<String>

        // TODO: calculate this based on screen size rather than magic numbers: https://chilipot.atlassian.net/browse/LIB-272
        abstract val numGenreOptionsToShow: Int

        data class Recommendations(
            override val genreOptions: List<String>,
            val recentlyRemovedGenres: Set<String> = emptySet()
        ) : Genres() {
            override val numGenreOptionsToShow = 28
        }

        data class Search(
            val searchQuery: String,
            override val genreOptions: List<String>
        ) : Genres() {
            override val numGenreOptionsToShow = 50
        }
    }
}
