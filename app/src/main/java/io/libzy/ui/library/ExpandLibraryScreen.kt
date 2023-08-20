package io.libzy.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration.Indefinite
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.ui.Destination
import io.libzy.ui.common.component.AlbumGrid
import io.libzy.ui.common.component.AlbumListItem
import io.libzy.ui.common.component.AlbumUiState
import io.libzy.ui.common.component.LibrarySyncProgress
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.LoadedContent
import io.libzy.ui.common.component.OpenSpotifyButton
import io.libzy.ui.common.util.LifecycleEffect
import io.libzy.ui.common.util.numItemsSeen
import io.libzy.ui.common.util.numVisibleItems
import io.libzy.ui.common.util.rememberedLayoutInfo
import io.libzy.ui.common.util.rememberedNumItemsSeen
import io.libzy.ui.common.util.scrollableFade
import io.libzy.ui.library.ExpandLibraryUiEvent.ExitApp
import io.libzy.ui.library.ExpandLibraryUiEvent.ForViewModel
import io.libzy.ui.library.ExpandLibraryUiEvent.Initialize
import io.libzy.ui.library.ExpandLibraryUiEvent.NavToQueryScreen
import io.libzy.ui.library.ExpandLibraryUiEvent.RecommendAlbums
import io.libzy.ui.library.ExpandLibraryUiEvent.Refresh
import io.libzy.ui.theme.LibzyDimens.FAB_REGION_HEIGHT
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import io.libzy.util.resolveText
import io.libzy.util.toTextResource
import java.util.UUID

@Composable
fun ExpandLibraryScreen(
    navController: NavController,
    viewModelFactory: ViewModelProvider.Factory,
    exitApp: () -> Unit
) {
    val viewModel: ExpandLibraryViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiStateFlow.collectAsState()

    ExpandLibraryScreen(uiState) { event ->
        when (event) {
            is NavToQueryScreen -> navController.popBackStack(Destination.Query.route, inclusive = false)
            is ExitApp -> exitApp()
            is ForViewModel -> viewModel.processEvent(event)
        }
    }
}

@Composable
private fun ExpandLibraryScreen(uiState: ExpandLibraryUiState, onUiEvent: (ExpandLibraryUiEvent) -> Unit) {
    val scaffoldState = rememberScaffoldState()
    val albumGridState = rememberLazyGridState()

    ExpandLibraryEventListeners(scaffoldState, albumGridState, uiState, onUiEvent)

    LoadedContent(uiState.loading) {
        LibzyScaffold(
            scaffoldState = scaffoldState,
            showTopBar = false,
            floatingActionButton = { if (!uiState.awaitingLibrarySync) OpenSpotifyButton() }
        ) {
            Crossfade(targetState = uiState.awaitingLibrarySync, label = "awaiting sync crossfade") { awaitingLibrarySync ->
                if (awaitingLibrarySync) {
                    LibrarySyncProgress()
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ExpandLibraryHeaders(uiState, onUiEvent)
                        RecommendedAlbums(uiState, albumGridState, Modifier.weight(1f), onUiEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandLibraryEventListeners(
    scaffoldState: ScaffoldState,
    albumGridState: LazyGridState,
    uiState: ExpandLibraryUiState,
    onUiEvent: (ExpandLibraryUiEvent) -> Unit
) {
    BackHandler { onUiEvent(uiState.backClickEvent) }
    LifecycleEffect(ON_CREATE) { onUiEvent(Initialize) }
    LifecycleEffect(ON_START) { onUiEvent(Refresh) }

    if (uiState.finishedAwaitingLibrarySync) {
        LaunchedEffect(Unit) {
            onUiEvent(NavToQueryScreen)
        }
    }

    val noNetworkMessage = stringResource(R.string.no_network_connection)
    val errorFetchingRecommendationsMessage = stringResource(R.string.error_fetching_recommendations)
    LaunchedEffect(uiState.networkConnected) {
        if (!uiState.networkConnected) {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            scaffoldState.snackbarHostState.showSnackbar(noNetworkMessage, duration = Indefinite)
        } else {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    // user is awaiting recommendations if they've seen loading placeholder albums at the end of the list
    val userAwaitingRecommendations = albumGridState.rememberedNumItemsSeen > (uiState.recommendedAlbums?.size ?: 0)

    if (uiState.networkConnected && uiState.errorFetchingRecommendations && userAwaitingRecommendations) {
        LaunchedEffect(Unit) {
            scaffoldState.snackbarHostState.showSnackbar(errorFetchingRecommendationsMessage, duration = Indefinite)
        }
    }
    if (uiState.networkConnected && (!uiState.errorFetchingRecommendations || !userAwaitingRecommendations)) {
        LaunchedEffect(Unit) {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
}

@Composable
private fun ExpandLibraryHeaders(uiState: ExpandLibraryUiState, onUiEvent: (ExpandLibraryUiEvent) -> Unit) {
    if (uiState.headerText != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = HORIZONTAL_INSET.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = uiState.headerText.resolveText(),
                style = MaterialTheme.typography.subtitle1.copy(lineBreak = LineBreak.Heading)
            )
            Spacer(Modifier.height(8.dp))

            val savedAlbumsTextAlpha = remember { Animatable(0f) }
            if (uiState.savedAlbumsText != null) {
                LaunchedEffect(Unit) {
                    savedAlbumsTextAlpha.animateTo(1f)
                }
            }
            val savedAlbumsText = uiState.savedAlbumsText?.resolveText()
                ?: pluralStringResource(R.plurals.albums_in_your_collection, 0, 0)
            Text(
                text = savedAlbumsText,
                style = MaterialTheme.typography.subtitle2,
                color = Color.Gray,
                modifier = Modifier.alpha(savedAlbumsTextAlpha.value)
            )

            if (uiState.enoughAlbumsSaved) {
                TextButton(
                    onClick = { onUiEvent(uiState.doneSavingEvent) },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.finish_saving_albums_button))
                    Icon(LibzyIconTheme.ArrowForward, contentDescription = null, Modifier.padding(start = 8.dp))
                }
            } else {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RecommendedAlbums(
    uiState: ExpandLibraryUiState,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    onUiEvent: (ExpandLibraryUiEvent) -> Unit
) = with(uiState) {
    BoxWithConstraints(modifier) {
        val numRecommendedAlbums = recommendedAlbums?.size ?: 0
        val numColumns = gridState.rememberedLayoutInfo.visibleItemsInfo.maxOfOrNull { it.column + 1 } ?: 1
        val numEmptyEndColumns = numRecommendedAlbums % numColumns
        var numInitialPlaceholderAlbums by remember { mutableIntStateOf(100) }
        val numPlaceholderAlbums = when {
            recommendedAlbums.isNullOrEmpty() -> numInitialPlaceholderAlbums
            else -> (numColumns + numEmptyEndColumns).coerceAtLeast(numInitialPlaceholderAlbums - numRecommendedAlbums)
        }

        if (recommendedAlbums.isNullOrEmpty()) {
            LaunchedEffect(gridState) {
                numInitialPlaceholderAlbums = gridState.numVisibleItems
            }
        }

        LaunchedEffect(gridState, numRecommendedAlbums, lastRecommendationPageSize, fetchingMoreRecommendations) {
            snapshotFlow { gridState.numItemsSeen }.collect { numItemsSeen ->
                if (!fetchingMoreRecommendations && numRecommendedAlbums - numItemsSeen < INFINITE_SCROLL_BUFFER) {
                    onUiEvent(RecommendAlbums)
                }
            }
        }
        AlbumGrid(
            albums = recommendedAlbums.orEmpty(),
            modifier = Modifier
                .fillMaxSize()
                .scrollableFade(
                    topFadeHeight = 16.dp,
                    bottomFadeHeight = FAB_REGION_HEIGHT.dp
                ),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = FAB_REGION_HEIGHT.dp + 16.dp
            ),
            onUiEvent = {
                if (it is ExpandLibraryUiEvent) {
                    onUiEvent(it)
                }
            },
            state = gridState
        ) {
            items(numPlaceholderAlbums) {
                PlaceholderAlbum()
            }
        }
    }
}

@Composable
private fun PlaceholderAlbum() {
    AlbumListItem(
        album = remember {
            AlbumUiState(
                title = R.string.placeholder_album_title.toTextResource(),
                artists = R.string.placeholder_album_artists.toTextResource(),
                icon = LibzyIconTheme.FavoriteBorder,
                placeholderShimmer = true,
                spotifyId = UUID.randomUUID().toString()
            )
        }
    )
}

private const val INFINITE_SCROLL_BUFFER = 30