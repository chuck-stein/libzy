package io.libzy.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.libzy.ui.MainActivity

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
