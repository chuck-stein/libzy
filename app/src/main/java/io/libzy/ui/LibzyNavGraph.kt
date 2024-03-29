package io.libzy.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import io.libzy.ui.connect.ConnectSpotifyScreen
import io.libzy.ui.findalbum.query.QueryScreen
import io.libzy.ui.findalbum.results.ResultsScreen
import io.libzy.ui.library.ExpandLibraryScreen
import io.libzy.ui.onboarding.OnboardingScreen
import io.libzy.ui.settings.SettingsScreen

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LibzyNavGraph(uiState: SessionUiState, viewModelFactory: ViewModelProvider.Factory, exitApp: () -> Unit) {
    val navController = rememberNavController()

    /**
     * Helper for adding a [Destination] to the nav graph, with automatic redirects
     * if the pre-conditions for showing the given [Destination] are not met.
     */
    fun NavGraphBuilder.composable(destination: Destination, content: @Composable (NavBackStackEntry) -> Unit) {
        composable(destination.route, destination.arguments, destination.deepLinks) { backStackEntry ->
            val redirectToConnectSpotify = destination.requiresSpotifyConnection && !uiState.isSpotifyConnected
            val redirectToExpandLibrary = !redirectToConnectSpotify && destination.requiresEnoughAlbumsSaved && !uiState.areEnoughAlbumsSaved
            val redirectToOnboarding = !redirectToConnectSpotify && !redirectToExpandLibrary &&
                    destination.requiresOnboarding && !uiState.isOnboardingCompleted
            LaunchedEffect(redirectToConnectSpotify) {
                if (redirectToConnectSpotify) navController.navigate(Destination.ConnectSpotify.route)
            }
            LaunchedEffect(redirectToExpandLibrary) {
                if (redirectToExpandLibrary) navController.navigate(Destination.ExpandLibrary.route)
            }
            LaunchedEffect(redirectToOnboarding) {
                if (redirectToOnboarding) navController.navigate(Destination.Onboarding.route)
            }
            if (!redirectToConnectSpotify) content(backStackEntry)
        }
    }

    // TODO: add transition animations to/from each screen in the nav graph once they are supported (especially Results)
    NavHost(navController, route = Destination.NavHost.route, startDestination = Destination.FindAlbumFlow.route) {
        composable(Destination.ConnectSpotify) {
            ConnectSpotifyScreen(navController, viewModelFactory, exitApp)
        }
        composable(Destination.ExpandLibrary) {
            ExpandLibraryScreen(navController, viewModelFactory, exitApp)
        }
        composable(Destination.Onboarding) {
            OnboardingScreen(navController, viewModelFactory, exitApp)
        }
        navigation(route = Destination.FindAlbumFlow.route, startDestination = Destination.Query.route) {
            composable(Destination.Query) { backStackEntry ->
                QueryScreen(navController, viewModelFactory, backStackEntry)
            }
            composable(Destination.Results) { backStackEntry ->
                ResultsScreen(navController, viewModelFactory, backStackEntry)
            }
        }
        composable(Destination.Settings) {
            SettingsScreen(navController, viewModelFactory)
        }
    }
}
