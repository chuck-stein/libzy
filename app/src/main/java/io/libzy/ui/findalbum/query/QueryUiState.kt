package io.libzy.ui.findalbum.query

import io.libzy.domain.Query

data class QueryUiState(
    val querySteps: List<QueryStep> = defaultQuerySteps,
    val currentStepIndex: Int = 0,
    val previousStepIndex: Int? = null,
    val recommendedGenres: List<String> = emptyList(),
    val query: Query = Query()
) {
    val currentStep = querySteps[currentStepIndex]
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
