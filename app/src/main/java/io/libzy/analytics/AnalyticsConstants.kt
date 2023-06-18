package io.libzy.analytics

import io.libzy.domain.Query.Familiarity
import io.libzy.domain.RecommendationCategory
import io.libzy.ui.connect.ConnectSpotifyScreen
import io.libzy.ui.findalbum.query.QueryScreen
import io.libzy.ui.findalbum.results.ResultsScreen
import io.libzy.work.LibrarySyncWorker

/**
 * Constants related to analytics events, including event names, event properties, and user properties.
 */
object AnalyticsConstants {

    object Events {
        /**
         * When the user rates how accurate the given album results are to their query
         */
        const val RATE_ALBUM_RESULTS = "rate album results"

        /**
         * When a [LibrarySyncWorker] either fails, succeeds, or retries
         */
        const val SYNC_LIBRARY_DATA = "sync library data"

        /**
         * When the user submits a query representing what they are currently in the mood to listen to.
         */
        const val SUBMIT_QUERY = "submit query"

        /**
         * When the user views recommended albums on [ResultsScreen] based on a query they submitted.
         */
        const val VIEW_ALBUM_RESULTS = "view album results"

        /**
         * When the user plays an album from those recommended to them in [ResultsScreen].
         */
        const val PLAY_ALBUM = "play album"

        /**
         * When the user views a question in [QueryScreen].
         */
        const val VIEW_QUESTION = "view question"

        /**
         * When the user views [ConnectSpotifyScreen].
         */
        const val VIEW_CONNECT_SPOTIFY_SCREEN = "view connect spotify screen"

        /**
         * When the user clicks the "Connect Spotify" button in [ConnectSpotifyScreen].
         */
        const val CLICK_CONNECT_SPOTIFY = "click connect spotify"

        /**
         * When the user authorizes Libzy to connect to their Spotify account.
         */
        const val AUTHORIZE_SPOTIFY_CONNECTION = "authorize spotify connection"

        /**
         * When the user starts searching genres on [QueryScreen].
         */
        const val START_GENRE_SEARCH = "start genre search"

        /**
         * When the user stops searching genres on [QueryScreen].
         */
        const val STOP_GENRE_SEARCH = "stop genre search"

        /**
         * When the user selects a genre on [QueryScreen].
         */
        const val SELECT_GENRE = "select genre"

        /**
         * When the user deselects a genre on [QueryScreen].
         */
        const val DESELECT_GENRE = "deselect genre"

        /**
         * When the user dismisses the keyboard while searching genres on [QueryScreen].
         */
        const val DISMISS_KEYBOARD = "dismiss keyboard"

        /**
         * When the user clicks the "start over" button on [QueryScreen] or [ResultsScreen] to restart the
         * "find album" flow.
         */
        const val CLICK_START_OVER = "click start over"
    }

    object EventProperties {
        
        /**
         * An [Int] property representing a rating out of 5 stars that the user gives in [Events.RATE_ALBUM_RESULTS] 
         */
        const val RATING = "rating"

        /**
         * A nullable [String] property representing optional feedback to send along
         * with a rating in [Events.RATE_ALBUM_RESULTS]
         */
        const val FEEDBACK = "feedback"

        /**
         * A [Boolean] property representing whether or not [Events.SYNC_LIBRARY_DATA]
         * occurred during a user's initial library sync upon first connecting Spotify.
         */
        const val IS_INITIAL_SYNC = "is initial sync"

        /**
         * An [Int] property representing how many albums were synced in a successful [Events.SYNC_LIBRARY_DATA].
         * Null if the sync was not successful.
         */
        const val NUM_ALBUMS_SYNCED = "num albums synced"

        /**
         * A [Double] property representing the number of seconds it took for a successful [Events.SYNC_LIBRARY_DATA].
         * Null if the sync was not successful.
         */
        const val LIBRARY_SYNC_TIME = "library sync time"

        /**
         * A [String] property representing the result of [Events.SYNC_LIBRARY_DATA].
         * Should be a value from [LibrarySyncResult].
         */
        const val RESULT = "result"

        /**
         * A [String] property representing an informational message to go along with [Events.SYNC_LIBRARY_DATA].
         * For example, if the library sync failed, this message will include the reason for failure.
         */
        const val MESSAGE = "message"

        /**
         * A [String] property representing the user's selection of familiarity for [Events.SUBMIT_QUERY]
         * or [Events.VIEW_ALBUM_RESULTS]. Should be the name of one of the [Familiarity] enum values.
         * Null means no preference.
         */
        const val FAMILIARITY = "familiarity"

        /**
         * A [Boolean] property representing the user's selection of instrumental vs. vocal for [Events.SUBMIT_QUERY]
         * or [Events.VIEW_ALBUM_RESULTS]. Null means no preference.
         */
        const val INSTRUMENTAL = "instrumental"

        /**
         * A [Float] property representing the user's selection of acousticness for [Events.SUBMIT_QUERY]
         * or [Events.VIEW_ALBUM_RESULTS]. Null means no preference.
         *
         * A [Float] property representing the acousticness value of the album played for [Events.PLAY_ALBUM].
         */
        const val ACOUSTICNESS = "acousticness"

        /**
         * A [Float] property representing the user's selection of negative vs. positive emotion for
         * [Events.SUBMIT_QUERY] or [Events.VIEW_ALBUM_RESULTS]. Null means no preference.
         *
         * A [Float] property representing the valence value of the album played for [Events.PLAY_ALBUM].
         */
        const val VALENCE = "valence"

        /**
         * A [Float] property representing the user's selection of energy level for [Events.SUBMIT_QUERY]
         * or [Events.VIEW_ALBUM_RESULTS]. Null means no preference.
         *
         * A [Float] property representing the energy value of the album played for [Events.PLAY_ALBUM].
         */
        const val ENERGY = "energy"

        /**
         * A [Float] property representing the user's selection of danceable vs. arrhythmic for [Events.SUBMIT_QUERY]
         * or [Events.VIEW_ALBUM_RESULTS]. Null means no preference.
         *
         * A [Float] property representing the danceability value of the album played for [Events.PLAY_ALBUM].
         */
        const val DANCEABILITY = "danceability"

        /**
         * A [List] property representing the user's selection of genres for [Events.SUBMIT_QUERY]
         * or [Events.VIEW_ALBUM_RESULTS]. Null means no preference.
         *
         * A [Set] property representing the genres belonging to the album played for [Events.PLAY_ALBUM].
         */
        const val GENRES = "genres"

        /**
         * An [Int] property representing the number of genres a user selected for [Events.SUBMIT_QUERY]
         * or [Events.VIEW_ALBUM_RESULTS].
         */
        const val NUM_GENRES = "num genres"

        /**
         * A [List] property containing the title of each [RecommendationCategory] in the results
         * for [Events.VIEW_ALBUM_RESULTS].
         */
        const val CATEGORIES = "categories"

        /**
         * An [Int] property representing the number of [RecommendationCategory]s in the results
         * for [Events.VIEW_ALBUM_RESULTS].
         */
        const val NUM_CATEGORIES = "num categories"

        /**
         * An [Int] property representing the number of albums recommended as a result of the user's current mood query
         * for [Events.VIEW_ALBUM_RESULTS].
         */
        const val NUM_ALBUM_RESULTS = "num album results"

        /**
         * A [String] property representing the Spotify URI for the album played in [Events.PLAY_ALBUM].
         */
        const val SPOTIFY_URI = "spotify uri"

        /**
         * A [String] property representing the title of the album played in [Events.PLAY_ALBUM].
         */
        const val TITLE = "title"

        /**
         * A [String] property representing the artist of the album played in [Events.PLAY_ALBUM].
         * May be multiple artist names concatenated by commas.
         */
        const val ARTIST = "artist"

        /**
         * A [Float] property representing the instrumentalness value of the album played for [Events.PLAY_ALBUM].
         */
        const val INSTRUMENTALNESS = "instrumentalness"

        /**
         * A [Boolean] property representing whether the album played for [Events.PLAY_ALBUM] is a long term favorite
         * of the user.
         */
        const val IS_LONG_TERM_FAVORITE = "is long term favorite"

        /**
         * A [Boolean] property representing whether the album played for [Events.PLAY_ALBUM] is a medium term favorite
         * of the user.
         */
        const val IS_MEDIUM_TERM_FAVORITE = "is medium term favorite"

        /**
         * A [Boolean] property representing whether the album played for [Events.PLAY_ALBUM] is a short term favorite
         * of the user.
         */
        const val IS_SHORT_TERM_FAVORITE = "is short term favorite"

        /**
         * A [Boolean] property representing whether the user has recently played
         * the album played for [Events.PLAY_ALBUM].
         */
        const val IS_RECENTLY_PLAYED = "is recently played"

        /**
         * A [Boolean] property representing whether the album played for [Events.PLAY_ALBUM] is of relatively low
         * familiarity to the user, meaning it has not been recently played and is not a favorite of theirs over any
         * period of time.
         */
        const val IS_LOW_FAMILIARITY = "is low familiarity"

        /**
         * An [Int] property representing which question number in [QueryScreen]'s question sequence was
         * viewed for [Events.VIEW_QUESTION].
         */
        const val QUESTION_NUM = "question num"

        /**
         * An [Int] property representing the total number of questions in the question sequence for which a question
         * was viewed for [Events.VIEW_QUESTION].
         */
        const val TOTAL_QUESTIONS = "total questions"

        /**
         * A [String] property representing the name of the question viewed for [Events.VIEW_QUESTION].
         */
        const val QUESTION_NAME = "question name"

        /**
         * A [String] property representing the Spotify user ID that is currently connected to Libzy at the time of
         * [Events.CLICK_CONNECT_SPOTIFY]. Null if no Spotify account is currently connected.
         */
        const val CURRENTLY_CONNECTED_USER_ID = "currently connected user id"

        /**
         * A [Set] property representing the genres the user currently has selected for [Events.START_GENRE_SEARCH],
         * [Events.STOP_GENRE_SEARCH], [Events.SELECT_GENRE], [Events.DESELECT_GENRE], or [Events.DISMISS_KEYBOARD].
         */
        const val CURRENTLY_SELECTED_GENRES = "currently selected genres"

        /**
         * A [String] property representing the genre that has just been selected or deselected for
         * [Events.SELECT_GENRE] or [Events.DESELECT_GENRE].
         */
        const val GENRE = "genre"

        /**
         * A [String] property representing the genre search query currently entered at the time of
         * [Events.SELECT_GENRE], [Events.DESELECT_GENRE], [Events.DISMISS_KEYBOARD], or null if not currently searching.
         */
        const val CURRENT_SEARCH_QUERY = "current search query"

        /**
         * A [Boolean] property representing whether the user is currently searching genres
         * (or false if just viewing recommendations) for [Events.SELECT_GENRE] or [Events.DESELECT_GENRE].
         */
        const val CURRENTLY_SEARCHING = "currently searching"
    }
    
    object UserProperties {

        /**
         * An [Int] property representing the number of albums a user has saved in their Spotify library.
         */
        const val NUM_ALBUMS_IN_LIBRARY = "num albums in library"

        /**
         * A [String] property representing the user's Spotify display name
         */
        const val DISPLAY_NAME = "display name"

        /**
         * An [Int] property representing the total number of times this user has played an album from Libzy
         */
        const val NUM_ALBUM_PLAYS = "num album plays"

        /**
         * An [Int] property representing the total number of times this user has submitted a query
         */
        const val NUM_QUERIES_SUBMITTED = "num queries submitted"
    }
}
