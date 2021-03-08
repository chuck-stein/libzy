package io.libzy.analytics

import io.libzy.analytics.Analytics.EventProperties
import io.libzy.analytics.Analytics.Events

/**
 * Contains the possible values for [Events.SYNC_LIBRARY_DATA].
 */
enum class LibrarySyncResult(val value: String) {

    /**
     * The value for a [EventProperties.RESULT] of [Events.SYNC_LIBRARY_DATA],
     * signifying that the sync was successful.
     */
    SUCCESS("success"),

    /**
     * The value for a [EventProperties.RESULT] of [Events.SYNC_LIBRARY_DATA],
     * signifying that the sync failed and will not retry.
     */
    FAILURE("failure"),

    /**
     * The value for a [EventProperties.RESULT] of [Events.SYNC_LIBRARY_DATA],
     * signifying that the sync did not succeed but will retry.
     */
    RETRY("retry")
}