package io.libzy.ui.connect

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.ui.LibzyContent
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.LibzyButton
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.theme.LibzyColors
import io.libzy.ui.theme.LibzyDimens.CIRCULAR_PROGRESS_INDICATOR_SIZE
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import kotlinx.coroutines.launch

/**
 * **Stateful** Connect Spotify Screen, displaying either a button to connect the user's Spotify
 * account and sync their library, or the progress of the library sync if it has started.
 */
@Composable
fun ConnectSpotifyScreen(
    navController: NavController,
    viewModelFactory: ViewModelProvider.Factory,
    exitApp: () -> Unit
) {
    val viewModel: ConnectSpotifyViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiStateFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val syncFailedMsg = stringResource(R.string.snackbar_library_sync_failed)
    val spotifyAuthFailedMsg = stringResource(R.string.snackbar_spotify_authorization_failed)

    fun showSnackbar(message: String) {
        scope.launch {
            scaffoldState.snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    EventHandler(viewModel.uiEvents) {
        when (it) {
            ConnectSpotifyUiEvent.SPOTIFY_CONNECTED -> navController.popBackStack() // this screen is always a redirect
            ConnectSpotifyUiEvent.SPOTIFY_SYNC_FAILED -> showSnackbar(syncFailedMsg)
            ConnectSpotifyUiEvent.SPOTIFY_AUTHORIZATION_FAILED -> showSnackbar(spotifyAuthFailedMsg)
        }
    }

    BackHandler(onBack = exitApp)

    LaunchedEffect(Unit) {
        viewModel.sendScreenViewAnalyticsEvent()
    }

    ConnectSpotifyScreen(uiState = uiState, scaffoldState = scaffoldState, onConnectSpotifyClick = {
        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
        viewModel.onConnectSpotifyClick()
    })
}

/**
 * **Stateless** Connect Spotify Screen, displaying either a button to connect the user's Spotify
 * account and sync their library, or the progress of the library sync if it has started.
 */
@Composable
private fun ConnectSpotifyScreen(
    uiState: ConnectSpotifyUiState,
    scaffoldState: ScaffoldState,
    onConnectSpotifyClick: () -> Unit
) {
    LibzyScaffold(scaffoldState = scaffoldState, showTopBar = false) {
        Crossfade(targetState = uiState.librarySyncInProgress) { librarySyncInProgress ->
            if (!librarySyncInProgress) {
                WelcomePage(onConnectSpotifyClick)
            } else {
                LibrarySyncProgress()
            }
        }
    }
}

@Composable
private fun WelcomePage(onConnectSpotifyClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = LibzyIconTheme.LibraryMusic, // TODO: replace with actual app icon once done
                contentDescription = stringResource(R.string.cd_libzy_icon),
                tint = LibzyColors.OffWhite,
                modifier = Modifier.fillMaxSize(0.25f)
            )
            Text(stringResource(R.string.welcome_to_libzy), style = MaterialTheme.typography.h3)
            Text(
                stringResource(R.string.your_digital_record_collection),
                style = MaterialTheme.typography.h5
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 32.dp).fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.getting_started),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            LibzyButton(
                R.string.connect_spotify_button_text,
                onClick = onConnectSpotifyClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = LibzyColors.SpotifyBranding,
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
                startContent = {
                    Icon(
                        painterResource(R.drawable.ic_spotify_black),
                        contentDescription = null
                    )
                }
            )
        }
    }
}

// TODO: replace CircularProgressIndicator with LinearProgressIndicator(progress = X) where X is a float representing approximate progress,
//  based on number of albums synced (could also show some text indicating this) and number of other network operations completed
@Composable
private fun LibrarySyncProgress() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HORIZONTAL_INSET.dp)
    ) {
        Text(
            stringResource(R.string.syncing_library_heading),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.weight(0.45f).padding(top = 64.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.55f)) {
            CircularProgressIndicator(Modifier.size(CIRCULAR_PROGRESS_INDICATOR_SIZE.dp))
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.syncing_library_subheading), style = MaterialTheme.typography.h6)
        }
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true)
@Composable
private fun ConnectSpotifyScreenPreview() {
    LibzyContent {
        ConnectSpotifyScreen(
            uiState = ConnectSpotifyUiState(librarySyncInProgress = false),
            scaffoldState = rememberScaffoldState(),
            onConnectSpotifyClick = {}
        )
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true)
@Composable
private fun LibrarySyncingPreview() {
    LibzyContent {
        ConnectSpotifyScreen(
            uiState = ConnectSpotifyUiState(librarySyncInProgress = true),
            scaffoldState = rememberScaffoldState(),
            onConnectSpotifyClick = {}
        )
    }
}
