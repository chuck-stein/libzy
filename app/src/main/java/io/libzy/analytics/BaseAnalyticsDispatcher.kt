package io.libzy.analytics

interface BaseAnalyticsDispatcher {

    fun sendEvent(
        eventName: String,
        eventProperties: Map<String, Any?>? = null,
        outOfSession: Boolean = false
    )
}