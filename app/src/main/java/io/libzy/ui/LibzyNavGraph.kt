package io.libzy.ui

import android.net.Uri
import android.os.Bundle
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NamedNavArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import io.libzy.domain.Query
import io.libzy.ui.connect.ConnectSpotifyScreen
import io.libzy.ui.launch.LaunchScreen
import io.libzy.ui.query.QueryScreen
import io.libzy.ui.results.ResultsScreen

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LibzyNavGraph(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()

    // TODO: add transition animations to/from each screen in the nav graph once they are supported (especially Results)
    NavHost(navController, route = Destination.NavHost.route, startDestination = Destination.Launch.route) {
        destination(Destination.Launch) {
            LaunchScreen(navController, viewModelFactory)
        }
        destination(Destination.ConnectSpotify) {
            ConnectSpotifyScreen(navController, viewModelFactory)
        }
        destination(Destination.Query) {
            QueryScreen(navController, viewModelFactory)
        }
        destination(Destination.Results) {
            ResultsScreen(
                navController,
                viewModelFactory,
                navController.previousBackStackEntry?.arguments?.getParcelable(Destination.Results.QUERY_ARG) ?: Query()
            )
        }
    }
}

fun NavGraphBuilder.destination(destination: Destination, content: @Composable (NavBackStackEntry) -> Unit) {
    composable(destination.route, destination.arguments, destination.deepLinks, content)
}

/**
 * Represents a screen in the navigation graph, with a corresponding route, arguments, and deep links.
 */
sealed class Destination {
    abstract val route: String
    open val arguments: List<NamedNavArgument> = emptyList()
    open val deepLinks: List<NavDeepLink> = emptyList()

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
    object Launch : Destination() {
        override val route = "launch"
    }
    object ConnectSpotify : Destination() {
        override val route = "connect"
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }
    object Query : Destination() {
        override val route = "query"
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }
    object Results : Destination() {
        const val QUERY_ARG = "query"
        override val route = "results"

        fun NavController.navigateToResultsScreen(query: io.libzy.domain.Query) {
            // TODO: replace this navigation arg (and its counterpart above) with a shared VM scoped to nested nav graph
            currentBackStackEntry?.arguments = Bundle().apply {
                putParcelable(QUERY_ARG, query)
            }
            navigate(route)
        }
    }
}
