package io.libzy.ui

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NamedNavArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import io.libzy.ui.connect.ConnectSpotifyScreen
import io.libzy.ui.query.QueryScreen
import io.libzy.ui.results.ResultsScreen

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LibzyNavGraph(viewModelFactory: ViewModelProvider.Factory, isSpotifyConnected: () -> Boolean, exitApp: () -> Unit) {
    val navController = rememberNavController()

    // define a helper for adding destinations to the graph
    fun NavGraphBuilder.screen(destination: Destination, content: @Composable (NavBackStackEntry) -> Unit) {
        composable(destination.route, destination.arguments, destination.deepLinks) { backStackEntry ->
            val redirectToConnectSpotify = remember { destination.requiresSpotifyConnection && !isSpotifyConnected() }
            if (redirectToConnectSpotify) {
                navController.navigate(Destination.ConnectSpotify.route)
            } else {
                content(backStackEntry)
            }
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
            screen(Destination.Results) {
                ResultsScreen(navController, viewModelFactory)
            }
        }
    }
}

/**
 * Represents a screen in the navigation graph, with a corresponding route, arguments, and deep links.
 */
sealed class Destination {
    abstract val route: String
    open val arguments: List<NamedNavArgument> = emptyList()
    open val deepLinks: List<NavDeepLink> = emptyList()
    open val requiresSpotifyConnection = true

    protected fun createDeepLinkUri(): Uri = Uri.Builder()
        .scheme("libzy")
        .authority(route)
        .build()

    protected fun createDeepLinksFrom(vararg deepLinkUris: Uri) = deepLinkUris.map {
        navDeepLink {
            uriPattern = it.toString()
        }
    }

    object NavHost : Destination() {
        override val route = "host"
    }
    object ConnectSpotify : Destination() {
        override val route = "connect"
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
        override val requiresSpotifyConnection = false
    }
    object FindAlbumFlow : Destination() {
        override val route = "find-album"
    }
    object Query : Destination() {
        override val route = "query"
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }
    object Results : Destination() {
        override val route = "results"
    }
}
