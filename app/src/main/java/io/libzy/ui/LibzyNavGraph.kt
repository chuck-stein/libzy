package io.libzy.ui

import android.os.Bundle
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
        composable(Screen.Launch.route) {
            LaunchScreen(navController, viewModelFactory)
        }
        composable(Screen.ConnectSpotify.route) {
            ConnectSpotifyScreen(navController, viewModelFactory)
        }
        composable(Screen.Query.route) {
            QueryScreen(navController, viewModelFactory)
        }
        composable(Screen.Results.route) {
            ResultsScreen(
                navController,
                viewModelFactory,
                navController.previousBackStackEntry?.arguments?.getParcelable(Screen.Results.QUERY_ARG) ?: Query()
            )
        }
    }
}

/**
 * Represents a screen in the navigation graph, to centralize strings like routes and nav args.
 */
sealed interface Screen {
    val route: String

    object Launch : Screen {
        override val route = "launch"
    }
    object ConnectSpotify : Screen {
        override val route = "connect"
    }
    object Query : Screen {
        override val route = "query"
    }
    object Results : Screen {
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
