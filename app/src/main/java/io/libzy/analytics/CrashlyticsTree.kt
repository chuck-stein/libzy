package io.libzy.analytics

import android.util.Log.ASSERT
import android.util.Log.DEBUG
import android.util.Log.ERROR
import android.util.Log.INFO
import android.util.Log.VERBOSE
import android.util.Log.WARN
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

/**
 * A logging [Timber.Tree] which logs messages and reports any handled exceptions to Firebase Crashlytics.
 */
class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Firebase.crashlytics.log(createLogPrefix(priority) + message)
        if (t != null) Firebase.crashlytics.recordException(t)
    }

    private fun createLogPrefix(priority: Int) = when (priority) {
        VERBOSE -> "VERBOSE: "
        INFO -> "INFO: "
        DEBUG -> "DEBUG: "
        WARN -> "WARNING: "
        ERROR -> "ERROR: "
        ASSERT -> "ASSERT: "
        else -> ""
    }
}
