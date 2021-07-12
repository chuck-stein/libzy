package io.libzy.ui.query

import io.libzy.model.Query

// TODO: throw exception in initializer if current step is out of bounds of querySteps or currentStepIndex is negative
data class QueryUiState(
    val querySteps: List<QueryStep> = defaultQuerySteps,
    val currentStepIndex: Int = 0,
    val previousStepIndex: Int? = null,
    val recommendedGenres: List<String> = emptyList(),
    val query: Query = Query()
) {
    val currentStep = querySteps[currentStepIndex]
}

enum class QueryUiEvent {
    SUBMIT_QUERY
}

enum class QueryStep(val stringValue: String) {
    FAMILIARITY("familiarity"),
    INSTRUMENTALNESS("instrumentalness"),
    ACOUSTICNESS("acousticness"),
    VALENCE("valence"),
    ENERGY("energy"),
    DANCEABILITY("danceability"),
    GENRES("genres")
}

private val defaultQuerySteps = listOf(
    QueryStep.FAMILIARITY,
    QueryStep.INSTRUMENTALNESS,
    QueryStep.ACOUSTICNESS,
    QueryStep.VALENCE,
    QueryStep.ENERGY,
    QueryStep.DANCEABILITY,
    QueryStep.GENRES
)
