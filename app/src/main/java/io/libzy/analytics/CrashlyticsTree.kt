package io.libzy.analytics

import android.util.Log.DEBUG
import android.util.Log.ERROR
import android.util.Log.INFO
import android.util.Log.VERBOSE
import android.util.Log.WARN
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

/**
 * A logging [Timber.Tree] which logs WARN and ERROR level messages to Firebase Crashlytics,
 * along with any exceptions the log contains. Ignores INFO, DEBUG, and VERBOSE level logs.
 */
class CrashlyticsTree : Timber.Tree() {
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority !in listOf(WARN, ERROR)) return

        val logPrefix = if (priority == WARN) "WARNING: " else "ERROR: "
        Firebase.crashlytics.log(logPrefix + message)
        if (t != null) Firebase.crashlytics.recordException(t)
    }
    
}
