package io.libzy.ui.findalbum.results

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.ThumbsUpDown
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.ui.Destination
import io.libzy.ui.LibzyContent
import io.libzy.ui.common.component.ALBUM_LIST_ITEM_PADDING
import io.libzy.ui.common.component.AlbumGrid
import io.libzy.ui.common.component.AlbumListItem
import io.libzy.ui.common.component.AlbumUiState
import io.libzy.ui.common.component.AutoResizeText
import io.libzy.ui.common.component.BackIcon
import io.libzy.ui.common.component.DEFAULT_ALBUM_LIST_ITEM_WIDTH
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.Frame
import io.libzy.ui.common.component.LibzyIcon
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.LifecycleObserver
import io.libzy.ui.common.component.OpenSpotifyButton
import io.libzy.ui.common.component.StartOverIconButton
import io.libzy.ui.common.util.restartFindAlbumFlow
import io.libzy.ui.common.util.scrollableFade
import io.libzy.ui.findalbum.FindAlbumFlowViewModel
import io.libzy.ui.theme.LibzyDimens.CIRCULAR_PROGRESS_INDICATOR_SIZE
import io.libzy.ui.theme.LibzyDimens.FAB_BOTTOM_PADDING
import io.libzy.ui.theme.LibzyDimens.FAB_REGION_HEIGHT
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import io.libzy.util.resolveText
import io.libzy.util.toTextResource
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
    val findAlbumFlowViewModel: FindAlbumFlowViewModel = viewModel(
        viewModelStoreOwner = remember(backStackEntry) {
            navController.getBackStackEntry(Destination.FindAlbumFlow.route)
        },
        factory = viewModelFactory
    )
    val uiState by viewModel.uiStateFlow.collectAsState()
    val findAlbumFlowUiState by findAlbumFlowViewModel.uiStateFlow.collectAsState()

    val context = LocalContext.current
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
            viewModel.recommendAlbums(findAlbumFlowUiState.query, context.resources)
        },
        onStop = {
            viewModel.disconnectSpotifyAppRemote()
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
        onRateResultsDismissed = viewModel::dismissRateResultsDialog,
        onRateResultsClick = viewModel::openRateResultsDialog,
        onRateResultsSubmit = viewModel::rateResults
    )
}

/**
 * **Stateless** results screen, displaying a list of suggested albums
 * based on what the user indicated they are in the mood to listen to.
 */
@Composable
private fun ResultsScreen(
    uiState: ResultsUiState,
    scaffoldState: ScaffoldState,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onStartOverClick: () -> Unit,
    onRateResultsDismissed: () -> Unit,
    onRateResultsClick: () -> Unit,
    onRateResultsSubmit: (Int, String?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    LibzyScaffold(
        title = {
            if (uiState is ResultsUiState.Loaded && uiState.recommendationCategories.isNotEmpty()) {
                AutoResizeText(stringResource(R.string.recommended_albums_title))
            }
        },
        scaffoldState = scaffoldState,
        navigationIcon = { BackIcon(onBackClick) },
        actionIcons = {
            if (uiState is ResultsUiState.Loaded && uiState.recommendationCategories.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onRateResultsClick()
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                    }
                ) {
                    LibzyIcon(LibzyIconTheme.ThumbsUpDown, contentDescription = stringResource(R.string.rate_results))
                }
            }
            StartOverIconButton(onStartOverClick)
        },
        floatingActionButton = {
            if (uiState is ResultsUiState.Loaded) {
                OpenSpotifyButton(uri = uiState.currentAlbumUri, source = Destination.Results)
            }
        }
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
                            Column(
                                Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = HORIZONTAL_INSET.dp)
                                    .padding(bottom = FAB_BOTTOM_PADDING.dp)
                            ) {
                                Text(
                                    stringResource(R.string.no_results_header),
                                    style = MaterialTheme.typography.h4,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(Modifier.padding(8.dp))
                                Text(
                                    stringResource(R.string.no_results_description),
                                    style = MaterialTheme.typography.h6,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                        1 -> {
                            // TODO: if the one category is a partial match, indicate that somehow (since we won't have a category title)
                            AlbumGrid(
                                albums = uiState.recommendationCategories.first().albums
                                    .map { it.copy(clickEvent = it.spotifyUri) }, // TODO: assign proper events in VM
                                modifier = Modifier.resultsGradient(),
                                contentPadding = PaddingValues(
                                    top = RECOMMENDATION_LIST_TOP_PADDING.dp,
                                    bottom = FAB_REGION_HEIGHT.dp + RECOMMENDATION_CATEGORY_BOTTOM_PADDING.dp
                                ),
                                onUiEvent = {
                                    if (it is String) {
                                        onAlbumClick(it)
                                    }
                                }
                            )
                        }
                        else -> {
                            AlbumResultsCategories(
                                uiState.recommendationCategories, onAlbumClick, Modifier.resultsGradient()
                            )
                        }
                    }
                    val feedbackSubmittedText = stringResource(R.string.results_feedback_submitted)
                    if (uiState.submittingFeedback) {
                        ResultsFeedbackDialog(onRateResultsDismissed) { rating, feedback ->
                            onRateResultsSubmit(rating, feedback)
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    message = feedbackSubmittedText,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.resultsGradient() = scrollableFade(
    topFadeHeight = RECOMMENDATION_LIST_TOP_PADDING.dp,
    bottomFadeHeight = RECOMMENDATION_LIST_BOTTOM_GRADIENT_HEIGHT.dp
)

@Composable
private fun AlbumResultsCategories(
    recommendationCategories: List<RecommendationCategoryUiState>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = RECOMMENDATION_LIST_TOP_PADDING.dp, bottom = FAB_REGION_HEIGHT.dp
        )
    ) {
        items(recommendationCategories) { category ->
            Column(modifier = Modifier.padding(bottom = RECOMMENDATION_CATEGORY_BOTTOM_PADDING.dp)) {
                Text(
                    text = category.title.resolveText(),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(horizontal = HORIZONTAL_INSET.dp)
                )
                LazyRow(contentPadding = PaddingValues(horizontal = (HORIZONTAL_INSET - ALBUM_LIST_ITEM_PADDING).dp)) {
                    items(category.albums.size) { albumIndex ->
                        val albumResult = category.albums[albumIndex]
                        AlbumListItem(
                            album = albumResult.copy(clickEvent = albumResult.spotifyUri), // TODO: set events from VM
                            modifier = Modifier.width(DEFAULT_ALBUM_LIST_ITEM_WIDTH.dp),
                            onAlbumClick = {
                                if (it is String) {
                                    onAlbumClick(it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsFeedbackDialog(
    onRateResultsDismissed: () -> Unit,
    onRateResultsSubmit: (Int, String?) -> Unit
) {
    var resultsRating: Int? by remember { mutableStateOf(null) }
    var resultsFeedback: String? by remember { mutableStateOf(null) }
    val dialogContentTextStyle = MaterialTheme.typography.body2.copy(textAlign = TextAlign.Start)
    val feedbackSubmittable = resultsRating != null

    fun submitFeedback() {
        resultsRating?.let { onRateResultsSubmit(it, resultsFeedback) }
    }

    AlertDialog(
        onDismissRequest = onRateResultsDismissed,
        confirmButton = {
            TextButton(
                onClick = ::submitFeedback,
                enabled = feedbackSubmittable
            ) {
                Text(stringResource(R.string.action_submit).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = onRateResultsDismissed) {
                Text(stringResource(R.string.action_cancel).uppercase())
            }
        },
        title = {
            Text(stringResource(R.string.rate_results))
        },
        text = {
            val focusManager = LocalFocusManager.current

            Column {
                Text(stringResource(R.string.rate_results_prompt), style = dialogContentTextStyle)
                RatingBar(
                    rating = resultsRating, onStarPress = { resultsRating = it }, modifier = Modifier
                        .padding(vertical = FEEDBACK_DIALOG_CONTENT_VERTICAL_PADDING.dp)
                        .align(Alignment.CenterHorizontally)
                )
                TextField(
                    value = resultsFeedback ?: "",
                    textStyle = dialogContentTextStyle,
                    onValueChange = { resultsFeedback = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.results_feedback_placeholder),
                            style = dialogContentTextStyle
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = if (feedbackSubmittable) ImeAction.Send else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { submitFeedback() },
                        onDone = { focusManager.clearFocus(force = true) }
                    ),
                    modifier = Modifier.height(FEEDBACK_TEXT_FIELD_HEIGHT.dp)
                )
            }
        }
    )
}

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

private val previewAlbum = AlbumUiState(
    title = "Album Title".toTextResource(),
    artists = "Album Artist".toTextResource(),
    artworkUrl = "https://i.scdn.co/image/8b662d81966a0ec40dc10563807696a8479cd48b0",
    spotifyId = ""
)

private val previewRecommendationCategories = List(4) { index ->
    RecommendationCategoryUiState(
        title = "Genre $index".toTextResource(),
        albums = List(5) { previewAlbum }
    )
}

private val previewRecommendationsOneCategory = listOf(
    RecommendationCategoryUiState(
        title = "Best Overall Match".toTextResource(),
        albums = List(20) { previewAlbum }
    )
)

@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun ResultsScreenPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(previewRecommendationCategories),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResultsDismissed = {},
            onRateResultsClick = {},
            onRateResultsSubmit = { _, _ -> }
        )
    }
}

@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun ResultsScreenOneCategoryPixel4XlPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(previewRecommendationsOneCategory),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResultsDismissed = {},
            onRateResultsClick = {},
            onRateResultsSubmit = { _, _ -> }
        )
    }
}

@Preview(device = Devices.PIXEL_C)
@Composable
private fun ResultsScreenOneCategoryPixelCPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(previewRecommendationsOneCategory),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResultsDismissed = {},
            onRateResultsClick = {},
            onRateResultsSubmit = { _, _ -> }
        )
    }
}

@Preview(device = Devices.PIXEL_3A)
@Composable
private fun ResultsScreenOneCategoryPixel3APreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(previewRecommendationsOneCategory),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResultsDismissed = {},
            onRateResultsClick = {},
            onRateResultsSubmit = { _, _ -> }
        )
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun ResultsScreenOneCategoryNexus5Preview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(previewRecommendationsOneCategory),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResultsDismissed = {},
            onRateResultsClick = {},
            onRateResultsSubmit = { _, _ -> }
        )
    }
}

@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun NoResultsScreenPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(recommendationCategories = emptyList()),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResultsDismissed = {},
            onRateResultsClick = {},
            onRateResultsSubmit = { _, _ -> }
        )
    }
}

@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun ResultsScreenFeedbackDialogPreview() {
    LibzyContent {
        ResultsScreen(
            uiState = ResultsUiState.Loaded(
                submittingFeedback = true,
                recommendationCategories = previewRecommendationCategories
            ),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onStartOverClick = {},
            onRateResultsDismissed = {},
            onRateResultsClick = {},
            onRateResultsSubmit = { _, _ -> }
        )
    }
}

private const val RECOMMENDATION_CATEGORY_BOTTOM_PADDING = 28
private const val RECOMMENDATION_LIST_TOP_PADDING = 16
private const val RECOMMENDATION_LIST_BOTTOM_GRADIENT_HEIGHT =
    FAB_REGION_HEIGHT + RECOMMENDATION_CATEGORY_BOTTOM_PADDING
private const val FEEDBACK_DIALOG_CONTENT_VERTICAL_PADDING = 12
private const val FEEDBACK_TEXT_FIELD_HEIGHT = 72
