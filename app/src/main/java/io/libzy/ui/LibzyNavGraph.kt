package io.libzy.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LibzyNavGraph(viewModelFactory: ViewModelProvider.Factory, isSpotifyConnected: () -> Boolean, exitApp: () -> Unit) {
    val navController = rememberNavController()

    // define a helper for adding destinations to the graph
    fun NavGraphBuilder.screen(destination: Destination, content: @Composable (NavBackStackEntry) -> Unit) {
        composable(destination.route, destination.arguments, destination.deepLinks) { backStackEntry ->
            val redirectToConnectSpotify = remember { destination.requiresSpotifyConnection && !isSpotifyConnected() }
            LaunchedEffect(redirectToConnectSpotify) {
                if (redirectToConnectSpotify) navController.navigate(Destination.ConnectSpotify.route)
            }
            if (!redirectToConnectSpotify) content(backStackEntry)
        }
    }

    // TODO: add transition animations to/from each screen in the nav graph once they are supported (especially Results)
    NavHost(navController, route = Destination.NavHost.route, startDestination = Destination.FindAlbumFlow.route) {
        screen(Destination.ConnectSpotify) {
            ConnectSpotifyScreen(navController, viewModelFactory, exitApp)
        }
        navigation(route = Destination.FindAlbumFlow.route, startDestination = Destination.Query.route) {
            screen(Destination.Query) {
                QueryScreen(navController, viewModelFactory)
            }
            screen(Destination.Results) { backStackEntry ->
                ResultsScreen(navController, viewModelFactory, backStackEntry)
            }
        }
    }
}
