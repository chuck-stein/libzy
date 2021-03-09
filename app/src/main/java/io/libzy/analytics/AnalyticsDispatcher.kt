package io.libzy.analytics

import com.amplitude.api.Amplitude
import com.amplitude.api.Identify
import io.libzy.analytics.Analytics.EventProperties.ACOUSTICNESS
import io.libzy.analytics.Analytics.EventProperties.ALBUM_RESULTS
import io.libzy.analytics.Analytics.EventProperties.DANCEABILITY
import io.libzy.analytics.Analytics.EventProperties.ENERGY
import io.libzy.analytics.Analytics.EventProperties.FAMILIARITY
import io.libzy.analytics.Analytics.EventProperties.GENRES
import io.libzy.analytics.Analytics.EventProperties.INSTRUMENTAL
import io.libzy.analytics.Analytics.EventProperties.IS_INITIAL_SCAN
import io.libzy.analytics.Analytics.EventProperties.LIBRARY_SYNC_TIME
import io.libzy.analytics.Analytics.EventProperties.NUM_ALBUMS_SYNCED
import io.libzy.analytics.Analytics.EventProperties.NUM_ALBUM_RESULTS
import io.libzy.analytics.Analytics.EventProperties.NUM_GENRES
import io.libzy.analytics.Analytics.EventProperties.RATING
import io.libzy.analytics.Analytics.EventProperties.RESULT
import io.libzy.analytics.Analytics.EventProperties.VALENCE
import io.libzy.analytics.Analytics.Events.RATE_ALBUM_RESULTS
import io.libzy.analytics.Analytics.Events.SUBMIT_QUERY
import io.libzy.analytics.Analytics.Events.SYNC_LIBRARY_DATA
import io.libzy.analytics.Analytics.UserProperties.DISPLAY_NAME
import io.libzy.model.AlbumResult
import io.libzy.model.Query
import io.libzy.util.toString
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Wrapper for Amplitude's SDK, used for dispatching analytics events via a simple API.
 * Contains a method for each event that can be dispatched,
 * with any method parameters needed to send along event properties.
 */
@Singleton
class AnalyticsDispatcher @Inject constructor() {

    private val amplitude = Amplitude.getInstance()

    fun setUserId(userId: String) {
        amplitude.userId = userId
    }

    fun setUserDisplayName(displayName: String) {
        val displayNameIdentification = Identify().set(DISPLAY_NAME, displayName)
        amplitude.identify(displayNameIdentification)
    }

    /**
     * Send an event with the given name and properties to Amplitude.
     */
    private fun sendEvent(eventName: String, eventProperties: Map<String, Any?>, outOfSession: Boolean = false) {
        amplitude.logEvent(eventName, JSONObject(eventProperties), outOfSession)
    }

    fun sendRateAlbumResultsEvent(rating: Int) {
        sendEvent(RATE_ALBUM_RESULTS, mapOf(RATING to rating))
    }

    fun sendSubmitQueryEvent(query: Query, results: List<AlbumResult>) {
        sendEvent(SUBMIT_QUERY, mapOf(
            FAMILIARITY to query.familiarity?.value,
            INSTRUMENTAL to query.instrumental,
            ACOUSTICNESS to query.acousticness?.toString(FLOAT_PRECISION),
            VALENCE to query.valence?.toString(FLOAT_PRECISION),
            ENERGY to query.energy?.toString(FLOAT_PRECISION),
            DANCEABILITY to query.danceability?.toString(FLOAT_PRECISION),
            GENRES to query.genres,
            NUM_GENRES to (query.genres?.size ?: 0),
            ALBUM_RESULTS to results.map { "${it.artists} - ${it.title} (${it.spotifyUri})" },
            NUM_ALBUM_RESULTS to results.size
        ))
    }

    fun sendSyncLibraryDataEvent(
        result: LibrarySyncResult,
        isInitialScan: Boolean,
        numAlbumsSynced: Int? = null,
        librarySyncTime: Double? = null
    ) {
        val eventProperties = mapOf(
            RESULT to result.value,
            IS_INITIAL_SCAN to isInitialScan,
            NUM_ALBUMS_SYNCED to numAlbumsSynced,
            LIBRARY_SYNC_TIME to librarySyncTime?.roundToInt()
        )
        sendEvent(SYNC_LIBRARY_DATA, eventProperties, outOfSession = true)
    }
}

/**
 * The number of decimal places to which floating point event properties should be formatted for analytics events.
 */
private const val FLOAT_PRECISION = 2
