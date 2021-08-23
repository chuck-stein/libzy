package io.libzy.ui.findalbum.query

import io.libzy.R
import io.libzy.domain.Query

data class QueryUiState(
    val stepOrder: List<QueryStep.Type>,
    val currentStep: QueryStep,
    val previousStepIndex: Int? = null,
    val query: Query = Query()
) {
    val currentStepIndex = stepOrder.indexOfFirst { it == currentStep.type }
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
            QueryStep.Type.FAMILIARITY,
            QueryStep.Type.INSTRUMENTALNESS,
            QueryStep.Type.ACOUSTICNESS,
            QueryStep.Type.VALENCE,
            QueryStep.Type.ENERGY,
            QueryStep.Type.DANCEABILITY,
            QueryStep.Type.GENRES
        )
    }
}

sealed class QueryStep(val type: Type) {

    enum class Type(val stringValue: String) {
        FAMILIARITY("familiarity"),
        INSTRUMENTALNESS("instrumentalness"),
        ACOUSTICNESS("acousticness"),
        VALENCE("valence"),
        ENERGY("energy"),
        DANCEABILITY("danceability"),
        GENRES("genres")
    }

    object Familiarity : QueryStep(Type.FAMILIARITY)

    object Instrumentalness : QueryStep(Type.INSTRUMENTALNESS)

    object Acousticness : QueryStep(Type.ACOUSTICNESS)

    object Valence : QueryStep(Type.VALENCE)

    object Energy : QueryStep(Type.ENERGY)

    object Danceability : QueryStep(Type.DANCEABILITY)

    sealed class Genres : QueryStep(Type.GENRES) {
        abstract val genreOptions: List<String>
        abstract val recentlyRemovedGenres: Set<String>

        data class Recommendations(
            override val genreOptions: List<String>,
            override val recentlyRemovedGenres: Set<String> = emptySet()
        ) : Genres()

        data class Search(
            val searchQuery: String,
            override val genreOptions: List<String>,
            override val recentlyRemovedGenres: Set<String> = emptySet()
        ) : Genres()
    }
}
