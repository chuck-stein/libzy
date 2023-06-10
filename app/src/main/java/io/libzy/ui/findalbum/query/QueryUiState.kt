package io.libzy.ui.findalbum.query

import io.libzy.R
import io.libzy.domain.Query
import io.libzy.ui.findalbum.query.QueryUiEvent.Continue
import io.libzy.ui.findalbum.query.QueryUiEvent.SubmitQuery

data class QueryUiState(
    val stepOrder: List<Query.Parameter> = DEFAULT_STEP_ORDER,
    val currentStep: QueryStep = QueryStep.Familiarity,
    val previousStepIndex: Int? = null,
    val query: Query = Query(),
    val loading: Boolean = false
) {
    val currentStepIndex = stepOrder.indexOfFirst { it == currentStep.parameterType }
    val onFinalStep = currentStepIndex == stepOrder.lastIndex
    val showBackButton = currentStepIndex > 0 || currentStep is QueryStep.Genres.Search
    val continueButtonText = if (onFinalStep) R.string.ready_button else R.string.continue_button
    val continueButtonClickEvent = if (onFinalStep) SubmitQuery else Continue
    val continueButtonEnabled = when (currentStep) {
        is QueryStep.Familiarity -> query.familiarity != null
        is QueryStep.Instrumentalness -> query.instrumental != null
        is QueryStep.Acousticness -> query.acousticness != null
        is QueryStep.Valence -> query.valence != null
        is QueryStep.Energy -> query.energy != null
        is QueryStep.Danceability -> query.danceability != null
        is QueryStep.Genres -> query.genres != null
    }
    val actionIcon = when (currentStepIndex) {
        0 -> QueryScreenActionIcon.Settings
        else -> QueryScreenActionIcon.StartOver
    }.takeIf { currentStep !is QueryStep.Genres.Search }

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
            override val genreOptions: List<String> = emptyList(),
            val recentlyRemovedGenres: Set<String> = emptySet()
        ) : Genres() {
            override val numGenreOptionsToShow = 28
        }

        data class Search(
            override val genreOptions: List<String> = emptyList(),
            val searchQuery: String
        ) : Genres() {
            override val numGenreOptionsToShow = 50
        }
    }
}

enum class QueryScreenActionIcon {
    Settings,
    StartOver
}
