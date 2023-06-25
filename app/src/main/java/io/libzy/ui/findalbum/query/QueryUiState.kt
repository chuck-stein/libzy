package io.libzy.ui.findalbum.query

import io.libzy.domain.Query

data class QueryUiState(
    val stepOrder: List<Query.Parameter> = Query.Parameter.defaultOrder,
    val query: Query = Query(),
    val genreOptions: List<String> = emptyList(),
    val genreSearchState: GenreSearchState = GenreSearchState.NotSearching(),
    val loadingStepOrder: Boolean,
    val awaitingOnboarding: Boolean,
) {
    val loading = loadingStepOrder || awaitingOnboarding
    val genreSearchQuery = (genreSearchState as? GenreSearchState.Searching)?.searchQuery
    val searchingGenres = genreSearchState is GenreSearchState.Searching
    val selectedGenres = query.genres.orEmpty()
    val genresToDisplay = when (genreSearchState) {
        is GenreSearchState.Searching -> genreOptions
            .filter {
                genreSearchState.searchQuery.isBlank()
                        || it.contains(genreSearchState.searchQuery.trim(), ignoreCase = true)
            }.take(NUM_GENRES_TO_DISPLAY_WHILE_SEARCHING)
        is GenreSearchState.NotSearching -> genreOptions
            .take(NUM_GENRES_TO_DISPLAY_WHILE_NOT_SEARCHING)
            .toSet()
            .plus(selectedGenres.plus(genreSearchState.recentlyRemovedGenres).sorted())
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

// TODO: calculate these based on screen size rather than magic numbers: https://chilipot.atlassian.net/browse/LIB-272
private const val NUM_GENRES_TO_DISPLAY_WHILE_SEARCHING = 50
private const val NUM_GENRES_TO_DISPLAY_WHILE_NOT_SEARCHING = 28
