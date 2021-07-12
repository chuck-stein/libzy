package io.libzy.util.extensions

import android.app.PendingIntent
import android.content.Context
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import io.libzy.R
import io.libzy.ui.MainActivity

fun appInForeground() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

fun Fragment.spotifyConnected() =
    requireContext().getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        .getBoolean(getString(R.string.spotify_connected_key), false)

fun Context.createNotificationTapAction(@IdRes destinationResId: Int): PendingIntent =
    NavDeepLinkBuilder(this)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_graph)
        .setDestination(destinationResId)
        .createPendingIntent()

fun NavController.navigate(route: String, popUpToInclusive: String) {
    navigate(route) {
        popUpTo(popUpToInclusive) {
            inclusive = true
        }
    }
}
