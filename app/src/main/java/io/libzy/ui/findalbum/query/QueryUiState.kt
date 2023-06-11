package io.libzy.ui.findalbum.query

import io.libzy.domain.Query

data class QueryUiState(
    val stepOrder: List<Query.Parameter> = DEFAULT_STEP_ORDER,
    val query: Query = Query(),
    val loading: Boolean = false,
    val genreOptions: List<String> = emptyList(),
    val genreSearchState: GenreSearchState = GenreSearchState.NotSearching()
) {
    val genreSearchQuery = (genreSearchState as? GenreSearchState.Searching)?.searchQuery
    val searchingGenres = genreSearchState is GenreSearchState.Searching
    val selectedGenres = query.genres.orEmpty()
    val genresToDisplay = when (genreSearchState) {
        is GenreSearchState.Searching -> genreOptions
            .filter {
                genreSearchState.searchQuery.isBlank() || it.contains(genreSearchState.searchQuery, ignoreCase = true)
            }.take(28)
        is GenreSearchState.NotSearching -> genreOptions
            .take(50) // TODO: calculate this (and 28 above) based on screen size rather than magic numbers: https://chilipot.atlassian.net/browse/LIB-272
            .toSet()
            .plus(selectedGenres.plus(genreSearchState.recentlyRemovedGenres).sorted())
    }

    companion object {
        // TODO: decouple this from QueryUiState, and rename to something that makes sense when just referencing it as a set of params rather than an order
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

sealed interface GenreSearchState {
    data class NotSearching(val recentlyRemovedGenres: Set<String> = emptySet()) : GenreSearchState
    data class Searching(val searchQuery: String = "") : GenreSearchState
}

enum class QueryScreenActionIcon {
    Settings,
    StartOver,
    ClearSearchQuery
}
