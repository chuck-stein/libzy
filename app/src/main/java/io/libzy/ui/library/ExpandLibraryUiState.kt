package io.libzy.ui.library

import com.adamratzman.spotify.models.Album
import io.libzy.ui.common.component.AlbumUiState
import io.libzy.ui.library.ExpandLibraryUiEvent.AwaitLibrarySync
import io.libzy.ui.library.ExpandLibraryUiEvent.ExitApp
import io.libzy.util.TextResource

data class ExpandLibraryUiState(
    val headerText: TextResource? = null,
    val savedAlbumsText: TextResource? = null,
    val numAlbumsSaved: Int = 0,
    val initialNumAlbumsSaved: Int = 0,
    val recommendedAlbums: List<AlbumUiState>? = null,
    val albumsById: Map<String, Album> = emptyMap(),
    val lastRecommendationPageSize: Int = 0,
    val fetchingMoreRecommendations: Boolean = false,
    val enoughAlbumsSaved: Boolean = false,
    val enoughAlbumsSavedAndCached: Boolean = false,
    val backClickEvent: ExpandLibraryUiEvent = ExitApp,
    val doneSavingEvent: ExpandLibraryUiEvent = AwaitLibrarySync,
    val awaitingLibrarySync: Boolean = false,
    val networkConnected: Boolean = true,
    val errorFetchingRecommendations: Boolean = false,
    val loading: Boolean = true
) {
    val finishedAwaitingLibrarySync = awaitingLibrarySync && enoughAlbumsSavedAndCached
}