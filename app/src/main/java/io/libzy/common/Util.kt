package io.libzy.common

import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.analytics.ktx.ParametersBuilder
import io.libzy.R
import io.libzy.view.MainActivity
import kotlin.math.roundToInt

// TODO: break this file into specific utils, e.g. AndroidUtil, MathUtil

// NOTE: after January 18th, 2038, this function will need to change because it will only return Int.MAX_VALUE
fun currentTimeSeconds() = (System.currentTimeMillis() / 1000.0).roundToInt()

fun percentageToFloat(percentage: Int) = percentage / 100F

fun Fragment.spotifyConnected() =
    requireContext().getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        .getBoolean(getString(R.string.spotify_connected_key), false)

fun appInForeground() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

fun Context.createNotificationTapAction(@IdRes destinationResId: Int): PendingIntent =
    NavDeepLinkBuilder(this)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_graph)
        .setDestination(destinationResId)
        .createPendingIntent()

val ViewGroup.children: List<View>
    get() {
        val children = mutableListOf<View>()
        for (i in 0 until childCount) {
            children.add(getChildAt(i))
        }
        return children
    }

fun ParametersBuilder.param(key: String, value: Boolean) {
    val valueAsLong: Long = if (value) 1 else 0
    param(key, valueAsLong)
}

fun ParametersBuilder.param(key: String, value: Int) {
    param(key, value.toLong())
}

fun ParametersBuilder.param(key: String, value: Float) {
    param(key, value.toDouble())
}
