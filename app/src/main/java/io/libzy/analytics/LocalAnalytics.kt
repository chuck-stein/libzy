package io.libzy.analytics

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import timber.log.Timber

val LocalAnalytics: ProvidableCompositionLocal<BaseAnalyticsDispatcher> = staticCompositionLocalOf {

    object : BaseAnalyticsDispatcher {
        override fun sendEvent(eventName: String, eventProperties: Map<String, Any?>?, outOfSession: Boolean) {
            Timber.e("LocalAnalytics was not provided when a composable attempted to send analytics")
        }
    }
}

