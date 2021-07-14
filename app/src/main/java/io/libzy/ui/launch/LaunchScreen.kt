package io.libzy.ui.launch

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.ui.LibzyContent
import io.libzy.ui.Screen
import io.libzy.ui.common.component.Frame
import io.libzy.ui.theme.LibzyIconTheme
import io.libzy.util.navigate

/**
 * A **stateful** screen that displays the app logo while its ViewModel loads content necessary to use the app,
 * after which it will direct the user to the appropriate home screen.
 */
@Composable
fun LaunchScreen(navController: NavController, viewModelFactory: ViewModelProvider.Factory) {
    val viewModel: LaunchViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.uiState

    when (state) {
        LaunchUiState.NEEDS_SPOTIFY_CONNECTION ->
            navController.navigate(Screen.ConnectSpotify.route, popUpToInclusive = Screen.Launch.route)
        LaunchUiState.SPOTIFY_CONNECTED ->
            navController.navigate(Screen.Query.route, popUpToInclusive = Screen.Launch.route)
        LaunchUiState.LOADING -> SplashImage()
    }
}

@Composable
private fun SplashImage() {
    Frame {
        Icon(
            imageVector = LibzyIconTheme.LibraryMusic, // TODO: replace with actual app icon once done
            contentDescription = stringResource(R.string.cd_app_logo),
            tint = MaterialTheme.colors.surface,
            modifier = Modifier.fillMaxSize(0.25f)
        )
    }
}

@Preview("Small Phone", device = Devices.NEXUS_5)
@Preview("Large Phone", device = Devices.PIXEL_4_XL)
@Preview("Tablet", device = Devices.PIXEL_C)
@Composable
private fun SplashScreen() {
    LibzyContent {
        SplashImage()
    }
}
