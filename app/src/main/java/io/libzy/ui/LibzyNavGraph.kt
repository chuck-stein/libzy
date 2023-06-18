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
import io.libzy.ui.settings.SettingsScreen

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LibzyNavGraph(uiState: SessionUiState, viewModelFactory: ViewModelProvider.Factory, exitApp: () -> Unit) {
    val navController = rememberNavController()

    // define a helper for adding destinations to the graph
    fun NavGraphBuilder.composable(destination: Destination, content: @Composable (NavBackStackEntry) -> Unit) {
        composable(destination.route, destination.arguments, destination.deepLinks) { backStackEntry ->
            val redirectToConnectSpotify = destination.requiresSpotifyConnection && !uiState.isSpotifyConnected
            LaunchedEffect(redirectToConnectSpotify) {
                if (redirectToConnectSpotify) navController.navigate(Destination.ConnectSpotify.route)
            }
            if (!redirectToConnectSpotify) content(backStackEntry)
        }
    }

    // TODO: add transition animations to/from each screen in the nav graph once they are supported (especially Results)
    NavHost(navController, route = Destination.NavHost.route, startDestination = Destination.FindAlbumFlow.route) {
        composable(Destination.ConnectSpotify) {
            ConnectSpotifyScreen(navController, viewModelFactory, exitApp)
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
