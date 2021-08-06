package io.libzy.ui.findalbum.results

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.domain.AlbumResult
import io.libzy.ui.Destination
import io.libzy.ui.LibzyContent
import io.libzy.ui.common.component.BackIcon
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.Frame
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.LifecycleObserver
import io.libzy.ui.common.loadRemoteImage
import io.libzy.ui.findalbum.FindAlbumFlowViewModel
import io.libzy.ui.theme.LibzyColors
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import kotlinx.coroutines.launch

/**
 * **Stateful** results screen, displaying a list of suggested albums
 * based on what the user indicated they are in the mood to listen to.
 */
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun ResultsScreen(navController: NavController, viewModelFactory: ViewModelProvider.Factory) {
    val viewModel: ResultsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState

    val findAlbumFlowViewModel: FindAlbumFlowViewModel = viewModel(
        viewModelStoreOwner = navController.getBackStackEntry(Destination.FindAlbumFlow.route),
        factory = viewModelFactory
    )
    val findAlbumFlowUiState by findAlbumFlowViewModel.uiState

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
    onRateResults: (Int) -> Unit
) {
    LibzyScaffold(
        scaffoldState = scaffoldState,
        navigationIcon = { BackIcon(onBackClick) },
    ) {
        if (uiState.loading) {
            Frame {
                CircularProgressIndicator(Modifier.size(60.dp))
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = HORIZONTAL_INSET.dp)
            ) {
                val headerResId = if (uiState.albumResults.isNotEmpty()) R.string.results_header else R.string.no_results_header
                Text(
                    stringResource(headerResId),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                if (uiState.albumResults.isNotEmpty()) {
                    AlbumResultsGrid(uiState.albumResults, onAlbumClick, Modifier.weight(1f))
                    Text(
                        stringResource(R.string.results_rating_text),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
                    )
                    RatingBar(uiState.resultsRating, onRateResults, Modifier.padding(bottom = 12.dp))
                }
            }
        }
    }
}

// TODO: handle rotation by maintaining position such that still looking at albums that were previously on screen
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun AlbumResultsGrid(albumResults: List<AlbumResult>, onAlbumClick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(cells = GridCells.Adaptive(110.dp), modifier = modifier) {
        items(albumResults.size) { index ->
            val albumResult = albumResults[index]
            AlbumResultListItem(
                title = albumResult.title,
                artists = albumResult.artists,
                artworkUrl = albumResult.artworkUrl,
                modifier = if (albumResult.spotifyUri != null) {
                    Modifier.clickable { onAlbumClick(albumResult.spotifyUri) }
                } else {
                    Modifier
                }
            )
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun AlbumResultListItem(
    title: String,
    artists: String,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(8.dp)) {
        AlbumArtwork(artworkUrl)
        Text(
            text = title,
            style = MaterialTheme.typography.body2,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp)
        )
        Text(
            text = artists,
            style = MaterialTheme.typography.body2,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            color = LibzyColors.Gray
        )
    }
}

@ExperimentalAnimationApi
@Composable
private fun AlbumArtwork(artworkUrl: String?) {
    val artworkContentDescription = stringResource(R.string.cd_album_artwork)
    val artworkModifier = Modifier.size(100.dp)

    loadRemoteImage(artworkUrl).value?.let { artworkBitmap ->
        Image(
            bitmap = artworkBitmap,
            contentDescription = artworkContentDescription,
            modifier = artworkModifier
        )
    } ?: Image(
        painter = painterResource(R.drawable.placeholder_album_art),
        contentDescription = artworkContentDescription,
        modifier = artworkModifier
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
                modifier = Modifier.size(40.dp).pointerInput(onStarPress, starIndex) {
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
            uiState = ResultsUiState(
                loading = false,
                albumResults = listOf(
                    AlbumResult(
                        "Lateralus",
                        "Tool",
                        artworkUrl = "https://i.scdn.co/image/8b662d81966a0ec40dc10563807696a8479cd48b"
                    ),
                    AlbumResult(
                        "Blast Tyrant",
                        "Clutch",
                        artworkUrl = "https://i.scdn.co/image/07c323340e03e25a8e5dd5b9a8ec72b69c50089d"
                    ),
                    AlbumResult(
                        "Yeezus",
                        "Kanye West",
                        artworkUrl = "https://i.scdn.co/image/8b662d81966a0ec40dc10563807696a8479cd48b0"
                    )
                )
            ),
            scaffoldState = rememberScaffoldState(),
            onBackClick = {},
            onAlbumClick = {},
            onRateResults = {}
        )
    }
}
