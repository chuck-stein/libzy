package io.libzy.analytics

import com.amplitude.api.Amplitude
import io.libzy.analytics.Analytics.EventProperties.ACOUSTICNESS
import io.libzy.analytics.Analytics.EventProperties.ALBUM_RESULTS
import io.libzy.analytics.Analytics.EventProperties.ARTIST
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
import io.libzy.analytics.Analytics.EventProperties.SPOTIFY_URI
import io.libzy.analytics.Analytics.EventProperties.TITLE
import io.libzy.analytics.Analytics.EventProperties.VALENCE
import io.libzy.analytics.Analytics.Events.RATE_ALBUM_RESULTS
import io.libzy.analytics.Analytics.Events.SUBMIT_QUERY
import io.libzy.analytics.Analytics.Events.SYNC_LIBRARY_DATA
import io.libzy.model.AlbumResult
import io.libzy.model.Query
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for Amplitude's SDK, used for dispatching analytics events via a simple API.
 * Contains a method for each event that can be dispatched,
 * with any method parameters needed to send along event properties.
 */
@Singleton
class AnalyticsDispatcher @Inject constructor() {

    private val amplitude = Amplitude.getInstance()

    private fun sendEvent(eventName: String, eventProperties: Map<String, Any?>) {
        amplitude.logEvent(eventName, JSONObject(eventProperties))
    }

    fun sendRateAlbumResultsEvent(rating: Int) {
        sendEvent(RATE_ALBUM_RESULTS, mapOf(RATING to rating))
    }

    fun sendSubmitQueryEvent(query: Query, results: List<AlbumResult>) {

        fun convertAlbumResultToMap(album: AlbumResult) = mapOf(
            TITLE to album.title,
            ARTIST to album.artists,
            SPOTIFY_URI to album.spotifyUri
        )

        sendEvent(SUBMIT_QUERY, mapOf(
            FAMILIARITY to query.familiarity?.value,
            INSTRUMENTAL to query.instrumental,
            ACOUSTICNESS to query.acousticness,
            VALENCE to query.valence,
            ENERGY to query.energy,
            DANCEABILITY to query.danceability,
            GENRES to query.genres,
            NUM_GENRES to (query.genres?.size ?: 0),
            ALBUM_RESULTS to results.map { convertAlbumResultToMap(it) },
            NUM_ALBUM_RESULTS to results.size
        ))
    }

    fun sendSyncLibraryDataEvent(
        result: LibrarySyncResult,
        isInitialScan: Boolean,
        numAlbumsSynced: Int? = null,
        librarySyncTime: Double? = null
    ) {
        sendEvent(SYNC_LIBRARY_DATA, mapOf(
            RESULT to result,
            IS_INITIAL_SCAN to isInitialScan,
            NUM_ALBUMS_SYNCED to numAlbumsSynced,
            LIBRARY_SYNC_TIME to librarySyncTime
            )
        )
    }
}
