package io.libzy.analytics

import io.libzy.work.LibrarySyncWorker

object LibzyAnalytics {

    object Event {
        /** When the user rates how accurate the given album results are to their query */
        const val RATE_ALBUM_RESULTS = "rate_album_results"

        /** When a [LibrarySyncWorker] either fails or succeeds */
        const val SYNC_LIBRARY_DATA = "sync_library_data"

        /** When a [LibrarySyncWorker] retries */
        const val RETRY_LIBRARY_SYNC = "retry_library_sync"

        /** When the user submits a query representing their current mood */
        const val SUBMIT_QUERY = "submit_query"
    }

    object Param {
        /** A [Long] param representing rating out of 5 stars that the user gives in [Event.RATE_ALBUM_RESULTS] */
        const val ALBUM_RESULTS_RATING = "album_results_rating"

        /**
         * A [Boolean] param representing whether or not [Event.SYNC_LIBRARY_DATA] or [Event.RETRY_LIBRARY_SYNC]
         * occurred during a user's initial library scan upon first connecting Spotify.
         */
        const val IS_INITIAL_SCAN = "is_initial_scan"

        /** An [Int] param representing how many albums were synced in a successful [Event.SYNC_LIBRARY_DATA] */
        const val NUM_ALBUMS_SYNCED = "num_albums_synced"

        /** A [Double] param representing the number of seconds it took for a successful [Event.SYNC_LIBRARY_DATA] */
        const val LIBRARY_SYNC_TIME = "library_sync_time"

        /** TODO */
        const val QUERIED_FAMILIARITY = "queried_familiarity"

        /** TODO */
        const val QUERIED_INSTRUMENTAL = "queried_instrumentalness"

        /** TODO */
        const val QUERIED_ACOUSTICNESS = "queried_acousticness"

        /** TODO */
        const val QUERIED_VALENCE = "queried_valence"

        /** TODO */
        const val QUERIED_ENERGY = "queried_energy"

        /** TODO */
        const val QUERIED_DANCEABILITY = "queried_danceability"

        /** TODO */
        const val QUERIED_GENRES = "queried_genres"

        /** TODO */
        const val QUERIED_NUM_GENRES = "queried_num_genres"

        /** TODO */
        const val NUM_ALBUMS_QUERIED = "num_albums_queried"

        /** TODO */
        const val NUM_ALBUM_RESULTS = "num_album_results"

        /** TODO */
        const val ALBUM_RESULTS = "album_results"
    }

}