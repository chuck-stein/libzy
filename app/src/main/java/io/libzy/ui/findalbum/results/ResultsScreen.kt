package io.libzy.ui.findalbum.results

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.domain.AlbumResult
import io.libzy.domain.Query.Familiarity.CURRENT_FAVORITE
import io.libzy.domain.Query.Familiarity.RELIABLE_CLASSIC
import io.libzy.domain.Query.Familiarity.UNDERAPPRECIATED_GEM
import io.libzy.domain.RecommendationCategory
import io.libzy.ui.Destination
import io.libzy.ui.LibzyContent
import io.libzy.ui.common.component.BackIcon
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.Frame
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.LifecycleObserver
import io.libzy.ui.common.util.loadRemoteImage
import io.libzy.ui.common.util.restartFindAlbumFlow
import io.libzy.ui.findalbum.FindAlbumFlowViewModel
import io.libzy.ui.theme.LibzyColors
import io.libzy.ui.theme.LibzyDimens.CIRCULAR_PROGRESS_INDICATOR_SIZE
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import io.libzy.util.capitalizeAllWords
import io.libzy.util.joinToUserFriendlyString
import kotlinx.coroutines.launch

/**
 * **Stateful** results screen, displaying a list of suggested albums
 * based on what the user indicated they are in the mood to listen to.
 */
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun ResultsScreen(
    navController: NavController,
    viewModelFactory: ViewModelProvider.Factory,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: ResultsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState

    val findAlbumFlowViewModel: FindAlbumFlowViewModel = viewModel(
        viewModelStoreOwner = remember(backStackEntry) {
            navController.getBackStackEntry(Destination.FindAlbumFlow.route)
        },
        factory = viewModelFactory
    )
    val findAlbumFlowUiState by rememberSaveable(findAlbumFlowViewModel.uiState) { findAlbumFlowViewModel.uiState }

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val spotifyRemoteFailMsg = stringResource(R.string.toast_spotify_remote_failed)

    EventHandler(viewModel.uiEvents) {
        if (it == ResultsUiEvent.SPOTIFY_REMOTE_FAILURE) {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(spotifyRemoteFailMsg, duration = SnackbarDuration.Short)
            }
        }
    }

    LifecycleObserver(
        onStart = {
            viewModel.connectSpotifyAppRemote()
            viewModel.recommendAlbums(findAlbumFlowUiState.query)
        },
        onStop = {
            viewModel.disconnectSpotifyAppRemote()
            viewModel.sendResultsRating()
        }
    )

    DisposableEffect(viewModel) {
        onDispose {
            // We want to disconnect the Spotify remote when this screen leaves the composition,
            // in addition to when the lifecycle reaches the STOPPED state.
            viewModel.disconnectSpotifyAppRemote()
        }
    }

    ResultsScreen(
        uiState = uiState,
        scaffoldState = scaffoldState,
        onBackClick = navController::popBackStack,
        onAlbumClick = viewModel::playAlbum,
        onStartOverClick = {
            navController.restartFindAlbumFlow()
            findAlbumFlowViewModel.sendClickStartOverAnalyticsEvent()
        },
        onRateResults = viewModel::rateResults
    )
}

/**
 * **Stateless** results screen, displaying a list of suggested albums
 * based on what the user indicated they are in the mood to listen to.
 */
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun ResultsScreen(
    uiState: ResultsUiState,
    scaffoldState: ScaffoldState,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onStartOverClick: () -> Unit,
    onRateResults: (Int) -> Unit
) {
    LibzyScaffold(
        title = {
            if (uiState is ResultsUiState.Loaded) {
                Text(stringResource(R.string.recommended_albums_title))
            }
        },
        scaffoldState = scaffoldState,
        navigationIcon = { BackIcon(onBackClick) },
        floatingActionButton = {
            if (uiState is ResultsUiState.Loaded) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.start_over).uppercase()) },
                    onClick = onStartOverClick,
                    backgroundColor = MaterialTheme.colors.primary,
                    icon = {
                        Icon(
                            imageVector = LibzyIconTheme.RestartAlt,
                            contentDescription = null, // button text serves as adequate CD already
                        )
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) {

        when (uiState) {
            is ResultsUiState.Loading -> {
                Frame {
                    CircularProgressIndicator(Modifier.size(CIRCULAR_PROGRESS_INDICATOR_SIZE.dp))
                }
            }
            is ResultsUiState.Loaded -> {
                Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {
                    when (uiState.recommendationCategories.size) {
                        0 -> {
                            Text(
                                stringResource(R.string.no_results_header),
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = HORIZONTAL_INSET.dp)
                            )
                        }
                        1 -> {
                            // TODO: if the one category is a partial match, indicate that somehow (since we won't have a category title)
                            AlbumResultsGrid(
                                uiState.recommendationCategories.first().albumResults,
                                onAlbumClick,
                                Modifier.resultsGradient()
                            )
                        }
                        else -> {
                            AlbumResultsCategories(
                                uiState.recommendationCategories, onAlbumClick, Modifier.resultsGradient()
                            )
                        }
                        // TODO: decide how to incorporate rating UX
                        // RatingBox(uiState.resultsRating, onRateResults, Modifier.padding(bottom = 16.dp).align(Alignment.BottomCenter))
                    }
                }
            }
        }
    }
}

private fun Modifier.resultsGradient() = this
    .graphicsLayer { alpha = 0.99f } // workaround to enable alpha compositing
    .drawWithContent {
        drawContent()
        drawRect(
            // gradient to fade out top of recommendation list
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                endY = RECOMMENDATION_LIST_TOP_PADDING.dp.toPx(),
            ),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            // gradient to fade out bottom of recommendation list
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black,
                    Color.Black.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                startY = size.height - RECOMMENDATION_LIST_BOTTOM_GRADIENT_HEIGHT.dp.toPx()
            ),
            blendMode = BlendMode.DstIn,
        )
    }

// TODO: naming (could be AlbumResults, AlbumResultsList, Recommendations, RecommendationCategories, ResultCategories, etc) -- also make sure it makes sense with a corresponding name for AlbumResultsGrid, which will still exist for instance if everything is a "perfect match"
@ExperimentalAnimationApi
@Composable
private fun AlbumResultsCategories(
    recommendationCategories: List<RecommendationCategory>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier, contentPadding = PaddingValues(
        top = RECOMMENDATION_LIST_TOP_PADDING.dp, bottom = RECOMMENDATION_LIST_BOTTOM_PADDING.dp
    )) {
        items(recommendationCategories.size) { categoryIndex ->
            val category = recommendationCategories[categoryIndex]
            Column(modifier = Modifier.padding(bottom = RECOMMENDATION_CATEGORY_BOTTOM_PADDING.dp)) {
                Text(
                    category.title(),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(horizontal = HORIZONTAL_INSET.dp)
                )
                LazyRow(contentPadding = PaddingValues(horizontal = (HORIZONTAL_INSET - ALBUM_RESULT_PADDING).dp)) {
                    items(category.albumResults.size) { albumIndex ->
                        val albumResult = category.albumResults[albumIndex]
                        AlbumResultListItem(albumResult, onAlbumClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCategory.title() = when (relevance) {
    is RecommendationCategory.Relevance.Full -> stringResource(R.string.full_match_category_title)
    is RecommendationCategory.Relevance.Partial -> {
        val adjectiveString = relevance.adjectives.map { stringResource(it) }.joinToUserFriendlyString()
        val capitalizedGenre = relevance.genre?.capitalizeAllWords()
        val nounString = when (relevance.familiarity) {
            CURRENT_FAVORITE -> capitalizedGenre?.let { stringResource(R.string.current_genre_favorites, it) }
                ?: stringResource(R.string.current_favorites)
            RELIABLE_CLASSIC -> capitalizedGenre?.let { stringResource(R.string.reliable_genre_classics, it) }
                ?: stringResource(R.string.reliable_classics)
            UNDERAPPRECIATED_GEM -> capitalizedGenre?.let { stringResource(R.string.underappreciated_genre, it) }
                ?: stringResource(R.string.underappreciated_gems)
            null -> capitalizedGenre.orEmpty()
        }

        buildString {
            append(adjectiveString)
            if (adjectiveString.isNotEmpty() && nounString.isNotEmpty()) {
                append(" ")
            }
            append(nounString)
        }
    }
}

// TODO: handle rotation by maintaining position such that still looking at albums that were previously on screen
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun AlbumResultsGrid(
    albumResults: List<AlbumResult>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridWidth = LocalConfiguration.current.screenWidthDp - (HORIZONTAL_INSET * 2) + (ALBUM_RESULT_PADDING * 2)
    val numColumns = maxOf((gridWidth / MIN_ALBUM_RESULT_WIDTH), 1)

    LazyVerticalGrid(
        columns = GridCells.Fixed(numColumns),
        modifier = modifier.padding(horizontal = (HORIZONTAL_INSET - ALBUM_RESULT_PADDING).dp),
        contentPadding = PaddingValues(bottom = RECOMMENDATION_LIST_BOTTOM_PADDING.dp)
    ) {
        items(albumResults.size) { index ->
            val albumResult = albumResults[index]
            AlbumResultListItem(albumResult, onAlbumClick, width = gridWidth / numColumns)
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun AlbumResultListItem(
    albumResult: AlbumResult,
    onAlbumClick: (String) -> Unit,
    width: Int = DEFAULT_ALBUM_RESULT_WIDTH
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width.dp)
            .clickable { onAlbumClick(albumResult.spotifyUri) }
            .padding(ALBUM_RESULT_PADDING.dp)
    ) {
        AlbumArtwork(albumResult.artworkUrl, size = width - (ALBUM_RESULT_PADDING * 2))
        Text(
            text = albumResult.title,
            style = MaterialTheme.typography.body2,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = albumResult.artists,
            style = MaterialTheme.typography.body2,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            color = LibzyColors.Gray
        )
    }
}

@ExperimentalAnimationApi
@Composable
private fun AlbumArtwork(artworkUrl: String?, size: Int) {
    val artworkContentDescription = stringResource(R.string.cd_album_artwork)
    val artworkModifier = Modifier.size(size.dp)

    val artworkBitmap = loadRemoteImage(artworkUrl)
    if (artworkBitmap != null) {
        Image(artworkBitmap, artworkContentDescription, artworkModifier)
    } else {
        Image(painterResource(R.drawable.placeholder_album_art), artworkContentDescription, artworkModifier)
    }
}

// TODO: determine how to incorporate this into new results UX, or remove it altogether
@Composable
private fun RatingBox(resultsRating: Int?, onRateResults: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colors.secondaryVariant, shape = RoundedCornerShape(16.dp), elevation = 100.dp, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.results_rating_text),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            RatingBar(resultsRating, onRateResults, Modifier.padding(bottom = 10.dp))
        }
    }
}

// TODO: if we use this without RatingBox, add horizontal inset
@Composable
private fun RatingBar(rating: Int?, onStarPress: (Int) -> Unit, modifier: Modifier = Modifier, numStars: Int = 5) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        repeat(numStars) { starIndex ->
            val starNum = starIndex + 1
            val starFilled = rating != null && starNum <= rating
            val starImage = if (starFilled) LibzyIconTheme.StarRate else LibzyIconTheme.StarBorder
            val starCdResId = if (starFilled) R.string.cd_filled_rating_star else R.string.cd_unfilled_rating_star

            Icon(
                imageVector = starImage,
                contentDescription = stringResource(starCdResId),
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(onStarPress, starIndex) {
                        detectTapGestures(
                            onPress = { onStarPress(starNum) }
                        )
                    }
            )
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun ResultsScreenPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(
                recommendationCategories = List(4) { index ->
                    RecommendationCategory(
                        relevance = RecommendationCategory.Relevance.Partial(genre = "Genre $index"),
                        albumResults = List(5) {
                            AlbumResult(
                                "Album Title",
                                "Album Artist",
                                artworkUrl = "https://i.scdn.co/image/8b662d81966a0ec40dc10563807696a8479cd48b0",
                                spotifyUri = ""
                            )
                        }
                    )
                }
            ),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResults = {}
        )
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun NoResultsScreenPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(
                recommendationCategories = emptyList()
            ),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResults = {}
        )
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun ResultsScreenOneCategoryPixel4XlPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(
                recommendationCategories = listOf(
                    RecommendationCategory(
                        relevance = RecommendationCategory.Relevance.Full,
                        albumResults = List(20) {
                            AlbumResult(
                                "Album Title",
                                "Album Artist",
                                artworkUrl = "https://i.scdn.co/image/8b662d81966a0ec40dc10563807696a8479cd48b0",
                                spotifyUri = ""
                            )
                        }
                    )
                )
            ),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResults = {}
        )
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Preview(device = Devices.PIXEL_C)
@Composable
private fun ResultsScreenOneCategoryPixelCPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(
                recommendationCategories = listOf(
                    RecommendationCategory(
                        relevance = RecommendationCategory.Relevance.Full,
                        albumResults = List(20) {
                            AlbumResult(
                                "Album Title",
                                "Album Artist",
                                artworkUrl = "https://i.scdn.co/image/8b662d81966a0ec40dc10563807696a8479cd48b0",
                                spotifyUri = ""
                            )
                        }
                    )
                )
            ),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResults = {}
        )
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Preview(device = Devices.PIXEL_3A)
@Composable
private fun ResultsScreenOneCategoryPixel3APreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(
                recommendationCategories = listOf(
                    RecommendationCategory(
                        relevance = RecommendationCategory.Relevance.Full,
                        albumResults = List(20) {
                            AlbumResult(
                                "Album Title",
                                "Album Artist",
                                artworkUrl = "https://i.scdn.co/image/8b662d81966a0ec40dc10563807696a8479cd48b0",
                                spotifyUri = ""
                            )
                        }
                    )
                )
            ),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResults = {}
        )
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Preview(device = Devices.NEXUS_5)
@Composable
private fun ResultsScreenOneCategoryNexus5Preview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(
                recommendationCategories = listOf(
                    RecommendationCategory(
                        relevance = RecommendationCategory.Relevance.Full,
                        albumResults = List(20) {
                            AlbumResult(
                                "Album Title",
                                "Album Artist",
                                artworkUrl = "https://i.scdn.co/image/8b662d81966a0ec40dc10563807696a8479cd48b0",
                                spotifyUri = ""
                            )
                        }
                    )
                )
            ),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResults = {}
        )
    }
}

// all in DP
const val FLOATING_ACTION_BUTTON_HEIGHT = 48
const val FLOATING_ACTION_BUTTON_BOTTOM_PADDING = 16
const val RECOMMENDATION_CATEGORY_BOTTOM_PADDING = 28
const val RECOMMENDATION_LIST_TOP_PADDING = 16
const val RECOMMENDATION_LIST_BOTTOM_PADDING = FLOATING_ACTION_BUTTON_HEIGHT + FLOATING_ACTION_BUTTON_BOTTOM_PADDING
const val RECOMMENDATION_LIST_BOTTOM_GRADIENT_HEIGHT =
    RECOMMENDATION_LIST_BOTTOM_PADDING + RECOMMENDATION_CATEGORY_BOTTOM_PADDING
const val ALBUM_RESULT_PADDING = 10
const val MIN_ALBUM_RESULT_WIDTH = 150
const val DEFAULT_ALBUM_RESULT_WIDTH = 160
