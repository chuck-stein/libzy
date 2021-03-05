package io.libzy.view.query

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import io.libzy.BuildConfig
import io.libzy.analytics.LibzyAnalytics.Event.SUBMIT_QUERY
import io.libzy.analytics.LibzyAnalytics.Param.ALBUM_RESULTS
import io.libzy.analytics.LibzyAnalytics.Param.NUM_ALBUMS_QUERIED
import io.libzy.analytics.LibzyAnalytics.Param.NUM_ALBUM_RESULTS
import io.libzy.analytics.LibzyAnalytics.Param.QUERIED_ACOUSTICNESS
import io.libzy.analytics.LibzyAnalytics.Param.QUERIED_DANCEABILITY
import io.libzy.analytics.LibzyAnalytics.Param.QUERIED_ENERGY
import io.libzy.analytics.LibzyAnalytics.Param.QUERIED_FAMILIARITY
import io.libzy.analytics.LibzyAnalytics.Param.QUERIED_GENRES
import io.libzy.analytics.LibzyAnalytics.Param.QUERIED_INSTRUMENTAL
import io.libzy.analytics.LibzyAnalytics.Param.QUERIED_NUM_GENRES
import io.libzy.analytics.LibzyAnalytics.Param.QUERIED_VALENCE
import io.libzy.common.CombinedLiveData
import io.libzy.common.param
import io.libzy.model.Query
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.remote.SpotifyAppRemoteService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findParameterByName

class QueryResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val spotifyAppRemoteService: SpotifyAppRemoteService
) : ViewModel() {

    private val defaultQuery = Query()

    private val queryLiveData = MutableLiveData(defaultQuery)

    /**
     * Function to send the [SUBMIT_QUERY] analytics event with the correct parameters.
     * Updates whenever the recommendation algorithm runs, so that the most up-to-date
     * query input and output can be sent along as event parameters.
     */
    var sendSubmitQueryEvent: () -> Unit = {}

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

    // we should recalculate the recommendations from the user's library
    // whenever their query updates or their library updates
    private val recommendationInput = CombinedLiveData(queryLiveData, userLibraryRepository.libraryAlbums)

    val recommendedAlbums = recommendationInput.map { (query, libraryAlbums) ->
        recommendationService.recommendAlbums(query, libraryAlbums).also { recommendedAlbums ->
            sendSubmitQueryEvent = {
                Firebase.analytics.logEvent(SUBMIT_QUERY) {
                    if (query.familiarity != null) param(QUERIED_FAMILIARITY, query.familiarity.name)
                    if (query.instrumental != null) param(QUERIED_INSTRUMENTAL, query.instrumental)
                    if (query.acousticness != null) param(QUERIED_ACOUSTICNESS, query.acousticness)
                    if (query.valence != null) param(QUERIED_VALENCE, query.valence)
                    if (query.energy != null) param(QUERIED_ENERGY, query.energy)
                    if (query.danceability != null) param(QUERIED_DANCEABILITY, query.danceability)
                    if (query.genres != null) {
                        param(QUERIED_GENRES, query.genres.joinToString())
                        param(QUERIED_NUM_GENRES, query.genres.size)
                    }
                    param(NUM_ALBUMS_QUERIED, libraryAlbums.size)
                    param(NUM_ALBUM_RESULTS, recommendedAlbums.size)
                    param(ALBUM_RESULTS, recommendedAlbums.joinToString { "${it.artists} - ${it.title} (${it.spotifyUri})" })
                }
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
        genres.let { previousGenres ->
            if (previousGenres != null) genres = genreOptions.filter { previousGenres.contains(it) }.toSet()
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
        GlobalScope.launch {
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
