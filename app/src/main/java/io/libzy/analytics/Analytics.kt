package io.libzy.analytics

import io.libzy.model.Query.Familiarity
import io.libzy.work.LibrarySyncWorker

/**
 * Constants related to analytics events, including event names, event properties, and user properties.
 */
object Analytics {

    object Events {
        /** When the user rates how accurate the given album results are to their query */
        const val RATE_ALBUM_RESULTS = "rate album results"

        /** When a [LibrarySyncWorker] either fails or succeeds */
        const val SYNC_LIBRARY_DATA = "sync library data"

        /** When a [LibrarySyncWorker] retries */
        const val RETRY_LIBRARY_SYNC = "retry library sync"

        /** When the user submits a query representing what they are currently in the mood to listen to. */
        const val SUBMIT_QUERY = "submit query"
    }

    object EventProperties {
        
        /**
         * An [Int] property representing a rating out of 5 stars that the user gives in [Events.RATE_ALBUM_RESULTS] 
         */
        const val RATING = "rating"

        /**
         * A [Boolean] property representing whether or not [Events.SYNC_LIBRARY_DATA] or [Events.RETRY_LIBRARY_SYNC]
         * occurred during a user's initial library scan upon first connecting Spotify.
         */
        const val IS_INITIAL_SCAN = "is initial scan"

        /** An [Int] property representing how many albums were synced in a successful [Events.SYNC_LIBRARY_DATA] */
        const val NUM_ALBUMS_SYNCED = "num albums synced"

        /**
         * A [Double] property representing the number of seconds it took for a successful [Events.SYNC_LIBRARY_DATA]
         */
        const val LIBRARY_SYNC_TIME = "library sync time"

        /**
         * A [String] property representing the user's selection of familiarity for [Events.SUBMIT_QUERY].
         * Should be the name of one of the [Familiarity] enum values.
         * Null means no preference.
         */
        const val FAMILIARITY = "familiarity"

        /**
         * A [Boolean] property representing the user's selection of instrumental vs. vocal for [Events.SUBMIT_QUERY].
         * Null means no preference.
         */
        const val INSTRUMENTAL = "instrumental"

        /**
         * A [Boolean] property representing the user's selection of acousticness for [Events.SUBMIT_QUERY].
         * Null means no preference.
         */
        const val ACOUSTICNESS = "acousticness"

        /**
         * A [Float] property representing the user's selection of negative vs. positive emotion for
         * [Events.SUBMIT_QUERY]. Null means no preference.
         */
        const val VALENCE = "valence"

        /**
         * A [Float] property representing the user's selection of energy level for [Events.SUBMIT_QUERY].
         * Null means no preference.
         */
        const val ENERGY = "energy"

        /**
         * A [Float] property representing the user's selection of danceable vs. arrhythmic for [Events.SUBMIT_QUERY].
         * Null means no preference.
         */
        const val DANCEABILITY = "danceability"

        /**
         * A [List] property representing the user's selection of genres for [Events.SUBMIT_QUERY].
         * Null means no preference.
         */
        const val GENRES = "genres"

        /**
         * An [Int] property representing the number of genres a user selected for [Events.SUBMIT_QUERY].
         */
        const val NUM_GENRES = "num genres"

        /**
         * A [List] property representing the albums recommended as a result of the user's current mood query for
         * [Events.SUBMIT_QUERY].
         */
        const val ALBUM_RESULTS = "album results"

        /**
         * A [String] property for each element of the [ALBUM_RESULTS] property, representing the album title.
         */
        const val TITLE = "title"

        /**
         * A [String] property for each element of the [ALBUM_RESULTS] property, representing the album artist(s).
         */
        const val ARTIST = "artist"

        /**
         * A [String] property for each element of the [ALBUM_RESULTS] property, representing the album's spotify URI.
         */
        const val SPOTIFY_URI = "spotify uri"

        /**
         * An [Int] property representing the number of albums recommended as a result of the user's current mood query
         * for [Events.SUBMIT_QUERY].
         */
        const val NUM_ALBUM_RESULTS = "num album results"
    }
    
    object UserProperties {

        /**
         * An [Int] property representing the number of albums a user has saved in their Spotify library.
         */
        const val NUM_ALBUMS_IN_LIBRARY = "num albums in library"
        
    }

}
