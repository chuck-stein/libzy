package io.libzy.analytics

import com.amplitude.api.Amplitude
import com.amplitude.api.Identify
import io.libzy.analytics.AnalyticsConstants.EventProperties.ACOUSTICNESS
import io.libzy.analytics.AnalyticsConstants.EventProperties.ALBUM_RESULTS
import io.libzy.analytics.AnalyticsConstants.EventProperties.ARTIST
import io.libzy.analytics.AnalyticsConstants.EventProperties.DANCEABILITY
import io.libzy.analytics.AnalyticsConstants.EventProperties.ENERGY
import io.libzy.analytics.AnalyticsConstants.EventProperties.FAMILIARITY
import io.libzy.analytics.AnalyticsConstants.EventProperties.GENRES
import io.libzy.analytics.AnalyticsConstants.EventProperties.INSTRUMENTAL
import io.libzy.analytics.AnalyticsConstants.EventProperties.INSTRUMENTALNESS
import io.libzy.analytics.AnalyticsConstants.EventProperties.IS_INITIAL_SCAN
import io.libzy.analytics.AnalyticsConstants.EventProperties.IS_LONG_TERM_FAVORITE
import io.libzy.analytics.AnalyticsConstants.EventProperties.IS_LOW_FAMILIARITY
import io.libzy.analytics.AnalyticsConstants.EventProperties.IS_MEDIUM_TERM_FAVORITE
import io.libzy.analytics.AnalyticsConstants.EventProperties.IS_RECENTLY_PLAYED
import io.libzy.analytics.AnalyticsConstants.EventProperties.IS_SHORT_TERM_FAVORITE
import io.libzy.analytics.AnalyticsConstants.EventProperties.LIBRARY_SYNC_TIME
import io.libzy.analytics.AnalyticsConstants.EventProperties.NUM_ALBUMS_SYNCED
import io.libzy.analytics.AnalyticsConstants.EventProperties.NUM_ALBUM_RESULTS
import io.libzy.analytics.AnalyticsConstants.EventProperties.NUM_GENRES
import io.libzy.analytics.AnalyticsConstants.EventProperties.RATING
import io.libzy.analytics.AnalyticsConstants.EventProperties.RESULT
import io.libzy.analytics.AnalyticsConstants.EventProperties.SPOTIFY_URI
import io.libzy.analytics.AnalyticsConstants.EventProperties.TITLE
import io.libzy.analytics.AnalyticsConstants.EventProperties.VALENCE
import io.libzy.analytics.AnalyticsConstants.Events.PLAY_ALBUM
import io.libzy.analytics.AnalyticsConstants.Events.RATE_ALBUM_RESULTS
import io.libzy.analytics.AnalyticsConstants.Events.SUBMIT_QUERY
import io.libzy.analytics.AnalyticsConstants.Events.SYNC_LIBRARY_DATA
import io.libzy.analytics.AnalyticsConstants.UserProperties.DISPLAY_NAME
import io.libzy.analytics.AnalyticsConstants.UserProperties.NUM_ALBUMS_IN_LIBRARY
import io.libzy.analytics.AnalyticsConstants.UserProperties.NUM_ALBUM_PLAYS
import io.libzy.analytics.AnalyticsConstants.UserProperties.NUM_QUERIES_SUBMITTED
import io.libzy.model.AlbumResult
import io.libzy.model.Query
import io.libzy.repository.UserLibraryRepository
import io.libzy.util.toString
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
class AnalyticsDispatcher @Inject constructor(private val userLibraryRepository: UserLibraryRepository) {

    private val amplitude = Amplitude.getInstance()

    /**
     * Shorthand for updating Amplitude user properties at the end of a call chain on an Identify object
     */
    private fun Identify.updateUserProperties() {
        amplitude.identify(this)
    }

    /**
     * Shorthand for incrementing a numerical user property by 1
     */
    private fun Identify.increment(property: String) = add(property, 1)

    fun setUserId(userId: String) {
        amplitude.userId = userId
    }

    fun setUserDisplayName(displayName: String) {
        Identify().set(DISPLAY_NAME, displayName).updateUserProperties()
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
        Identify().increment(NUM_QUERIES_SUBMITTED).updateUserProperties()

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
        numAlbumsSynced?.let {
            Identify().set(NUM_ALBUMS_IN_LIBRARY, it).updateUserProperties()
        }

        val eventProperties = mapOf(
            RESULT to result.value,
            IS_INITIAL_SCAN to isInitialScan,
            NUM_ALBUMS_SYNCED to numAlbumsSynced,
            LIBRARY_SYNC_TIME to librarySyncTime?.roundToInt()
        )
        sendEvent(SYNC_LIBRARY_DATA, eventProperties, outOfSession = true)
    }

    fun sendPlayAlbumEvent(spotifyUri: String) {
        Identify().increment(NUM_ALBUM_PLAYS).updateUserProperties()

        GlobalScope.launch {
            val album = userLibraryRepository.getAlbumFromUri(spotifyUri)
            sendEvent(PLAY_ALBUM, mapOf(
                SPOTIFY_URI to album.spotifyUri,
                TITLE to album.title,
                ARTIST to album.artists,
                GENRES to album.genres,
                ACOUSTICNESS to album.audioFeatures.acousticness,
                DANCEABILITY to album.audioFeatures.danceability,
                ENERGY to album.audioFeatures.energy,
                INSTRUMENTALNESS to album.audioFeatures.instrumentalness,
                VALENCE to album.audioFeatures.valence,
                IS_LONG_TERM_FAVORITE to album.familiarity.longTermFavorite,
                IS_MEDIUM_TERM_FAVORITE to album.familiarity.mediumTermFavorite,
                IS_SHORT_TERM_FAVORITE to album.familiarity.shortTermFavorite,
                IS_RECENTLY_PLAYED to album.familiarity.recentlyPlayed,
                IS_LOW_FAMILIARITY to album.familiarity.isLowFamiliarity()
            ))
        }
    }
}

/**
 * The number of decimal places to which floating point event properties should be formatted for analytics events.
 */
private const val FLOAT_PRECISION = 2
