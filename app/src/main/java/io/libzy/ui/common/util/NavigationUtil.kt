package io.libzy.ui.common.util

import androidx.navigation.NavController
import io.libzy.ui.Destination

fun NavController.restartFindAlbumFlow() {
    // pop the entire flow off the back stack, removing saved state of any previous visit to query screen
    popBackStack(Destination.FindAlbumFlow.route, inclusive = true, saveState = false)
    // navigate to query screen with fresh state after removing the previous query screen state from backstack
    navigate(Destination.Query.route)
}
