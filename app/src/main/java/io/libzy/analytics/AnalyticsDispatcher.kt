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
import io.libzy.analytics.Analytics.EventProperties.NUM_ALBUM_RESULTS
import io.libzy.analytics.Analytics.EventProperties.NUM_GENRES
import io.libzy.analytics.Analytics.EventProperties.RATING
import io.libzy.analytics.Analytics.EventProperties.SPOTIFY_URI
import io.libzy.analytics.Analytics.EventProperties.TITLE
import io.libzy.analytics.Analytics.EventProperties.VALENCE
import io.libzy.analytics.Analytics.Events.RATE_ALBUM_RESULTS
import io.libzy.analytics.Analytics.Events.SUBMIT_QUERY
import io.libzy.model.AlbumResult
import io.libzy.model.Query
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

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
        
        val eventProperties = mapOf(
            FAMILIARITY to query.familiarity?.name,
            INSTRUMENTAL to query.instrumental,
            ACOUSTICNESS to query.acousticness,
            VALENCE to query.valence,
            ENERGY to query.energy,
            DANCEABILITY to query.danceability,
            GENRES to query.genres,
            NUM_GENRES to (query.genres?.size ?: 0),
            ALBUM_RESULTS to results.map { convertAlbumResultToMap(it) },
            NUM_ALBUM_RESULTS to results.size
        )
        
        sendEvent(SUBMIT_QUERY, eventProperties)
    }
    
}
