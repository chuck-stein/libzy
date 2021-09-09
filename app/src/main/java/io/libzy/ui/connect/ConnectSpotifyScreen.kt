package io.libzy.ui.connect

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.Frame
import io.libzy.ui.common.component.LibzyButton
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import kotlinx.coroutines.launch

/**
 * **Stateful** Connect Spotify Screen, displaying either a button to connect the user's Spotify
 * account and scan their library, or the progress of the library scan if it has started.
 */
@Composable
fun ConnectSpotifyScreen(
    navController: NavController,
    viewModelFactory: ViewModelProvider.Factory,
    exitApp: () -> Unit
) {
    val viewModel: ConnectSpotifyViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val scanFailedMsg = stringResource(R.string.snackbar_library_scan_failed)
    val spotifyAuthFailedMsg = stringResource(R.string.snackbar_spotify_authorization_failed)

    fun showSnackbar(message: String) {
        scope.launch {
            scaffoldState.snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    EventHandler(viewModel.uiEvents) {
        when (it) {
            ConnectSpotifyUiEvent.SPOTIFY_CONNECTED -> navController.popBackStack() // this screen is always a redirect
            ConnectSpotifyUiEvent.SPOTIFY_SCAN_FAILED -> showSnackbar(scanFailedMsg)
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
 * account and scan their library, or the progress of the library scan if it has started.
 */
@Composable
private fun ConnectSpotifyScreen(
    uiState: ConnectSpotifyUiState,
    scaffoldState: ScaffoldState,
    onConnectSpotifyClick: () -> Unit
) {
    LibzyScaffold(scaffoldState = scaffoldState) {
        Crossfade(targetState = uiState.libraryScanInProgress) { libraryScanInProgress ->
            if (!libraryScanInProgress) {
                ConnectSpotifyButton(onConnectSpotifyClick)
            } else {
                LibraryScanProgress()
            }
        }
    }
}

@Composable
private fun ConnectSpotifyButton(onConnectSpotifyClick: () -> Unit) {
    Frame {
        LibzyButton(R.string.connect_spotify_button_text, onClick = onConnectSpotifyClick)
    }
}

// TODO: replace CircularProgressIndicator with LinearProgressIndicator(progress = X) where X is a float representing approximate progress,
//  based on number of albums scanned (could also show some text indicating this) and number of other network operations completed
@Composable
private fun LibraryScanProgress() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(horizontal = HORIZONTAL_INSET.dp)
    ) {
        Text(
            stringResource(R.string.scanning_library_heading),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.weight(0.45f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.55f)) {
            CircularProgressIndicator(Modifier.size(60.dp))
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.scanning_library_subheading), style = MaterialTheme.typography.h6)
        }
    }
}
