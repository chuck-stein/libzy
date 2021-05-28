package io.libzy.ui.query

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import io.libzy.BuildConfig
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.model.Query
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.remote.SpotifyAppRemoteService
import io.libzy.util.CombinedLiveData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findParameterByName

class QueryResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val spotifyAppRemoteService: SpotifyAppRemoteService,
    private val analyticsDispatcher: AnalyticsDispatcher
) : ViewModel() {

    private val defaultQuery = Query()

    private val queryLiveData = MutableLiveData(defaultQuery)

    var familiarity = defaultQuery.familiarity
        set(value) {
            field = value
            updateQueryLiveData(Query::familiarity, value)
        }

    var instrumental = defaultQuery.instrumental
        set(value) {
            field = value
            updateQueryLiveData(Query::instrumental, value)
        }

    var acousticness = defaultQuery.acousticness
        set(value) {
            field = value
            updateQueryLiveData(Query::acousticness, value)
        }

    var valence = defaultQuery.valence
        set(value) {
            field = value
            updateQueryLiveData(Query::valence, value)
        }

    var energy = defaultQuery.energy
        set(value) {
            field = value
            updateQueryLiveData(Query::energy, value)
        }

    var danceability = defaultQuery.danceability
        set(value) {
            field = value
            updateQueryLiveData(Query::danceability, value)
        }

    var genres = defaultQuery.genres
        set(value) {
            field = value
            updateQueryLiveData(Query::genres, value)
        }

    /**
     * Set the value of [queryLiveData] to a copy of its current value
     * (or of [defaultQuery] if the current value is null),
     * except with the given [queryProperty] set to [newValue].
     *
     * If the resulting query would be the same as the current query,
     * do nothing, so that observers are not alerted unnecessarily.
     */
    private fun <T> updateQueryLiveData(queryProperty: KProperty1<Query, T>, newValue: T) {
        queryLiveData.value.let { currentQuery ->
            // if the given property already has the given value, just return
            if (currentQuery != null && queryProperty.get(currentQuery) == newValue) return@updateQueryLiveData
        }

        val queryToCopy = queryLiveData.value ?: defaultQuery

        // The copy() method on the Query object should have a parameter of the same name
        // as the Query property we are updating -- get a reference to this parameter
        // so that we can call copy() with that parameter set to the new value
        val queryCopyParameter = queryToCopy::copy.findParameterByName(queryProperty.name)
        checkNotNull(queryCopyParameter) { "Query#copy does not have a parameter called ${queryProperty.name}" }

        queryLiveData.value = queryToCopy::copy.callBy(mapOf(queryCopyParameter to newValue))
    }

    // TODO: delete this, just for debugging
    init {
        viewModelScope.launch {
            while (true) {
                delay(30000)
                queryLiveData.value = queryLiveData.value?.copy(acousticness = acousticness?.times(0.9f) ?: 1f) ?: defaultQuery
            }
        }
    }

    /**
     * Whether the user has submitted their query but the corresponding analytics event has not yet been sent.
     * The analytics event will be sent the next time [recommendedAlbums] updates, in order to provide the results
     * of the query as an event property.
     */
    var submitQueryEventPending = false

    // we should recalculate the recommendations from the user's library
    // whenever their query updates or their library updates
    private val recommendationInput = CombinedLiveData(queryLiveData, userLibraryRepository.libraryAlbums)

    val recommendedAlbums = recommendationInput.map { (query, libraryAlbums) ->
        recommendationService.recommendAlbums(query, libraryAlbums).also { results ->
            if (submitQueryEventPending) {
                analyticsDispatcher.sendSubmitQueryEvent(query, results)
                submitQueryEventPending = false
            }
        }
    }

    val recommendedGenres = recommendationInput.map { (query, libraryAlbums) ->
        recommendationService.recommendGenres(query, libraryAlbums)
    }

    // TODO: delete if unused
    val spotifyPlayerState = spotifyAppRemoteService.playerState
    val spotifyPlayerContext = spotifyAppRemoteService.playerContext

    fun updateSelectedGenres(genreOptions: List<String>) {
        genres?.let { previousGenres ->
            genres = genreOptions.filter { previousGenres.contains(it) }.toSet()
        }
    }

    fun connectSpotifyAppRemote(onFailure: () -> Unit) {
        spotifyAppRemoteService.connect(onFailure)
    }

    fun disconnectSpotifyAppRemote() {
        spotifyAppRemoteService.disconnect()
    }

    fun playAlbum(spotifyUri: String) {
        if (BuildConfig.DEBUG) logAlbumDetails(spotifyUri)
        spotifyAppRemoteService.playAlbum(spotifyUri)
    }

    private fun logAlbumDetails(spotifyUri: String) {
        viewModelScope.launch {
            val album = userLibraryRepository.getAlbumFromUri(spotifyUri)
            val albumDetails =
                """
                title: ${album.title}
                genres: ${album.genres}
                acousticness: ${album.audioFeatures.acousticness}
                danceability: ${album.audioFeatures.danceability}
                energy: ${album.audioFeatures.energy}
                instrumentalness: ${album.audioFeatures.instrumentalness}
                valence: ${album.audioFeatures.valence}
                longTermFavorite: ${album.familiarity.longTermFavorite}
                mediumTermFavorite: ${album.familiarity.mediumTermFavorite}
                shortTermFavorite: ${album.familiarity.shortTermFavorite}
                recentlyPlayed: ${album.familiarity.recentlyPlayed}
                lowFamiliarity: ${album.familiarity.isLowFamiliarity()}
                """
            Timber.d(albumDetails.trimIndent())
        }
    }
}
