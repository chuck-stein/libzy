package io.libzy.analytics

import android.app.Application
import android.content.Context
import com.amplitude.api.Amplitude
import com.amplitude.api.Identify
import io.libzy.analytics.AnalyticsConstants.EventProperties.ACOUSTICNESS
import io.libzy.analytics.AnalyticsConstants.EventProperties.ALBUM_RESULTS
import io.libzy.analytics.AnalyticsConstants.EventProperties.ARTIST
import io.libzy.analytics.AnalyticsConstants.EventProperties.CURRENTLY_CONNECTED_USER_ID
import io.libzy.analytics.AnalyticsConstants.EventProperties.CURRENTLY_SEARCHING
import io.libzy.analytics.AnalyticsConstants.EventProperties.CURRENTLY_SELECTED_GENRES
import io.libzy.analytics.AnalyticsConstants.EventProperties.CURRENT_SEARCH_QUERY
import io.libzy.analytics.AnalyticsConstants.EventProperties.DANCEABILITY
import io.libzy.analytics.AnalyticsConstants.EventProperties.ENERGY
import io.libzy.analytics.AnalyticsConstants.EventProperties.FAMILIARITY
import io.libzy.analytics.AnalyticsConstants.EventProperties.FROM_CURRENT_OPTIONS
import io.libzy.analytics.AnalyticsConstants.EventProperties.GENRE
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
import io.libzy.analytics.AnalyticsConstants.EventProperties.QUESTION_NAME
import io.libzy.analytics.AnalyticsConstants.EventProperties.QUESTION_NUM
import io.libzy.analytics.AnalyticsConstants.EventProperties.RATING
import io.libzy.analytics.AnalyticsConstants.EventProperties.RESULT
import io.libzy.analytics.AnalyticsConstants.EventProperties.SPOTIFY_URI
import io.libzy.analytics.AnalyticsConstants.EventProperties.TITLE
import io.libzy.analytics.AnalyticsConstants.EventProperties.TOTAL_QUESTIONS
import io.libzy.analytics.AnalyticsConstants.EventProperties.VALENCE
import io.libzy.analytics.AnalyticsConstants.Events.AUTHORIZE_SPOTIFY_CONNECTION
import io.libzy.analytics.AnalyticsConstants.Events.CLICK_CONNECT_SPOTIFY
import io.libzy.analytics.AnalyticsConstants.Events.CLICK_START_OVER
import io.libzy.analytics.AnalyticsConstants.Events.DESELECT_GENRE
import io.libzy.analytics.AnalyticsConstants.Events.DISMISS_KEYBOARD
import io.libzy.analytics.AnalyticsConstants.Events.PLAY_ALBUM
import io.libzy.analytics.AnalyticsConstants.Events.RATE_ALBUM_RESULTS
import io.libzy.analytics.AnalyticsConstants.Events.SELECT_GENRE
import io.libzy.analytics.AnalyticsConstants.Events.START_GENRE_SEARCH
import io.libzy.analytics.AnalyticsConstants.Events.STOP_GENRE_SEARCH
import io.libzy.analytics.AnalyticsConstants.Events.SUBMIT_QUERY
import io.libzy.analytics.AnalyticsConstants.Events.SYNC_LIBRARY_DATA
import io.libzy.analytics.AnalyticsConstants.Events.VIEW_ALBUM_RESULTS
import io.libzy.analytics.AnalyticsConstants.Events.VIEW_CONNECT_SPOTIFY_SCREEN
import io.libzy.analytics.AnalyticsConstants.Events.VIEW_QUESTION
import io.libzy.analytics.AnalyticsConstants.UserProperties.DISPLAY_NAME
import io.libzy.analytics.AnalyticsConstants.UserProperties.NUM_ALBUMS_IN_LIBRARY
import io.libzy.analytics.AnalyticsConstants.UserProperties.NUM_ALBUM_PLAYS
import io.libzy.analytics.AnalyticsConstants.UserProperties.NUM_QUERIES_SUBMITTED
import io.libzy.domain.AlbumResult
import io.libzy.domain.Query
import io.libzy.persistence.prefs.SharedPrefKeys
import io.libzy.persistence.prefs.getSharedPrefs
import io.libzy.repository.UserLibraryRepository
import io.libzy.util.plus
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
class AnalyticsDispatcher @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    appContext: Context
) {
    private val amplitude = Amplitude.getInstance()

    private val sharedPrefs = appContext.getSharedPrefs()

    fun initialize(application: Application, apiKey: String) {
        amplitude
            .trackSessionEvents(true)
            .initialize(application, apiKey)
            .enableForegroundTracking(application)

        sharedPrefs.getString(SharedPrefKeys.SPOTIFY_USER_ID, null)?.let { setUserId(it) }
    }

    // ~~~~~~~~~~~~~~~~~~ User Properties  ~~~~~~~~~~~~~~~~~~

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

    // ~~~~~~~~~~~~~~~~~~ Events ~~~~~~~~~~~~~~~~~~

    /**
     * Send an event with the given name and properties to Amplitude.
     */
    private fun sendEvent(
        eventName: String,
        eventProperties: Map<String, Any?>? = null,
        outOfSession: Boolean = false
    ) {
        val eventPropertiesJson = eventProperties?.let { JSONObject(eventProperties) }
        amplitude.logEvent(eventName, eventPropertiesJson, outOfSession)
    }

    fun sendRateAlbumResultsEvent(rating: Int) {
        sendEvent(RATE_ALBUM_RESULTS, mapOf(RATING to rating))
    }

    fun sendSubmitQueryEvent(query: Query) {
        Identify().increment(NUM_QUERIES_SUBMITTED).updateUserProperties()

        sendEvent(SUBMIT_QUERY, query.toEventPropertyMap())
    }

    fun sendViewAlbumResultsEvent(query: Query, results: List<AlbumResult>) {
        sendEvent(VIEW_ALBUM_RESULTS, query.toEventPropertyMap().plus(
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

    fun sendViewQuestionEvent(questionName: String, questionNum: Int, totalQuestions: Int) {
        sendEvent(VIEW_QUESTION, mapOf(
            QUESTION_NUM to questionNum,
            QUESTION_NAME to questionName,
            TOTAL_QUESTIONS to totalQuestions
        ))
    }

    fun sendViewConnectSpotifyScreenEvent() {
        sendEvent(VIEW_CONNECT_SPOTIFY_SCREEN)
    }

    fun sendClickConnectSpotifyEvent(currentlyConnectedUserId: String?) {
        sendEvent(CLICK_CONNECT_SPOTIFY, mapOf(CURRENTLY_CONNECTED_USER_ID to currentlyConnectedUserId))
    }

    fun sendAuthorizeSpotifyConnectionEvent() {
        sendEvent(AUTHORIZE_SPOTIFY_CONNECTION)
    }

    fun sendStartGenreSearchEvent(currentlySelectedGenres: Set<String>) {
        sendEvent(START_GENRE_SEARCH, mapOf(CURRENTLY_SELECTED_GENRES to currentlySelectedGenres))
    }

    fun sendStopGenreSearchEvent(currentlySelectedGenres: Set<String>) {
        sendEvent(STOP_GENRE_SEARCH, mapOf(CURRENTLY_SELECTED_GENRES to currentlySelectedGenres))
    }

    fun sendSelectGenreEvent(
        genre: String,
        fromCurrentOptions: Boolean,
        currentlySearching: Boolean,
        currentSearchQuery: String?,
        currentlySelectedGenres: Set<String>
    ) {
        sendEvent(SELECT_GENRE, mapOf(
            GENRE to genre,
            FROM_CURRENT_OPTIONS to fromCurrentOptions,
            CURRENTLY_SEARCHING to currentlySearching,
            CURRENT_SEARCH_QUERY to currentSearchQuery,
            CURRENTLY_SELECTED_GENRES to currentlySelectedGenres
        ))
    }

    fun sendDeselectGenreEvent(
        genre: String,
        fromCurrentOptions: Boolean,
        currentlySearching: Boolean,
        currentSearchQuery: String?,
        currentlySelectedGenres: Set<String>
    ) {
        sendEvent(DESELECT_GENRE, mapOf(
            GENRE to genre,
            FROM_CURRENT_OPTIONS to fromCurrentOptions,
            CURRENTLY_SEARCHING to currentlySearching,
            CURRENT_SEARCH_QUERY to currentSearchQuery,
            CURRENTLY_SELECTED_GENRES to currentlySelectedGenres
        ))
    }

    fun sendDismissKeyboardEvent(currentSearchQuery: String, currentlySelectedGenres: Set<String>) {
        sendEvent(DISMISS_KEYBOARD, mapOf(
            CURRENT_SEARCH_QUERY to currentSearchQuery,
            CURRENTLY_SELECTED_GENRES to currentlySelectedGenres
        ))
    }

    fun sendClickStartOverEvent() {
        sendEvent(CLICK_START_OVER)
    }

    // ~~~~~~~~~~~~~~~~~~ Helpers ~~~~~~~~~~~~~~~~~~

    private fun Query.toEventPropertyMap() = mapOf(
        FAMILIARITY to familiarity?.value,
        INSTRUMENTAL to instrumental,
        ACOUSTICNESS to acousticness?.toString(FLOAT_PRECISION),
        VALENCE to valence?.toString(FLOAT_PRECISION),
        ENERGY to energy?.toString(FLOAT_PRECISION),
        DANCEABILITY to danceability?.toString(FLOAT_PRECISION),
        GENRES to genres,
        NUM_GENRES to (genres?.size ?: 0)
    )
}

/**
 * The number of decimal places to which floating point event properties should be formatted for analytics events.
 */
private const val FLOAT_PRECISION = 2
