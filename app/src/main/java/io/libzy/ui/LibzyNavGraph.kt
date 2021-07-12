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
import io.libzy.model.Query
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
    NavHost(navController, startDestination = Screen.Launch.route) {
        destination(Screen.Launch) {
            LaunchScreen(navController, viewModelFactory)
        }
        destination(Screen.ConnectSpotify) {
            ConnectSpotifyScreen(navController, viewModelFactory)
        }
        destination(Screen.Query) {
            QueryScreen(navController, viewModelFactory)
        }
        destination(Screen.Results) {
            ResultsScreen(
                navController,
                viewModelFactory,
                navController.previousBackStackEntry?.arguments?.getParcelable(Screen.Results.QUERY_ARG) ?: Query()
            )
        }
    }
}

fun NavGraphBuilder.destination(screen: Screen, content: @Composable (NavBackStackEntry) -> Unit) {
    composable(screen.route, screen.arguments, screen.deepLinks, content)
}

/**
 * Represents a screen in the navigation graph, with a corresponding route, arguments, and deep links.
 */
sealed class Screen {
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

    object Launch : Screen() {
        override val route = "launch"
    }
    object ConnectSpotify : Screen() {
        override val route = "connect"
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }
    object Query : Screen() {
        override val route = "query"
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }
    object Results : Screen() {
        const val QUERY_ARG = "query"
        override val route = "results"

        fun NavController.navigateToResultsScreen(query: io.libzy.model.Query) {
            // TODO: replace this navigation arg (and its counterpart above) with a shared VM scoped to nested nav graph
            currentBackStackEntry?.arguments = Bundle().apply {
                putParcelable(QUERY_ARG, query)
            }
            navigate(route)
        }
    }
}
