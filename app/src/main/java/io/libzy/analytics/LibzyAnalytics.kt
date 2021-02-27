package io.libzy.analytics

import io.libzy.work.RefreshLibraryWorker

object LibzyAnalytics {

    object Event {
        /** When the user rates how accurate the given album results are to their query */
        const val RATE_ALBUM_RESULTS = "rate_album_results"

        /** When a [RefreshLibraryWorker] either fails or succeeds */
        const val SYNC_LIBRARY_DATA = "sync_library_data"

        /** When a [RefreshLibraryWorker] retries */
        const val RETRY_LIBRARY_SYNC = "retry_library_sync"
    }

    object Param {
        /** A [Long] param representing rating out of 5 stars that the user gives in [Event.RATE_ALBUM_RESULTS] */
        const val ALBUM_RESULTS_RATING = "album_results_rating"

        /**
         * A [Boolean] param representing whether or not [Event.SYNC_LIBRARY_DATA] or [Event.RETRY_LIBRARY_SYNC]
         * occurred during a user's initial library sync upon first connecting Spotify.
         */
        const val IS_INITIAL_SYNC = "is_initial_sync"

        /** An [Int] param representing how many albums were synced in a successful [Event.SYNC_LIBRARY_DATA] */
        const val NUM_ALBUMS_SYNCED = "num_albums_synced"

        /** A [Double] param representing the number of seconds it took for a successful [Event.SYNC_LIBRARY_DATA] */
        const val LIBRARY_SYNC_TIME = "library_sync_time"
    }

}