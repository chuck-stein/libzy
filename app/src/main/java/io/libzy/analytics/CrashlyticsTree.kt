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
 * A logging [Timber.Tree] which logs INFO, WARN, and ERROR level messages to Firebase Crashlytics,
 * along with any exceptions the log contains. Ignores DEBUG and VERBOSE level logs.
 */
class CrashlyticsTree : Timber.Tree() {
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == VERBOSE || priority == DEBUG) return
        
        val messagePrefix = when(priority) {
            INFO -> "INFO: "
            WARN -> "WARNING: "
            ERROR -> "ERROR: "
            else -> ""
        }
        
        Firebase.crashlytics.log(messagePrefix + message)
        
        if (t != null) Firebase.crashlytics.recordException(t)
    }
    
}
