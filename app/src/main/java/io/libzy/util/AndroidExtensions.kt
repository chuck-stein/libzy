package io.libzy.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.libzy.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

fun appInForeground() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

fun PackageManager.isPackageInstalled(packageName: String) =
    try {
        getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

fun androidAppUriFor(packageName: String): Uri = Uri.parse("android-app://$packageName")

fun Context.createNotificationTapAction(uri: Uri): PendingIntent? {
    val intent = Intent(Intent.ACTION_VIEW, uri, this, MainActivity::class.java)
    return with(TaskStackBuilder.create(this)) {
        addNextIntentWithParentStack(intent)
        getPendingIntent(uri.hashCode(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

fun ConnectivityManager.connectedToNetwork() =
    getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

fun ConnectivityManager.networkConnectedFlow(): Flow<Boolean> = callbackFlow {
    trySend(connectedToNetwork())

    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            trySend(true)
        }
        override fun onLost(network: Network) {
            super.onLost(network)
            trySend(false)
        }
    }
    registerDefaultNetworkCallback(networkCallback)

    awaitClose {
        unregisterNetworkCallback(networkCallback)
    }
}.buffer(Channel.CONFLATED).flowOn(Dispatchers.Default)
