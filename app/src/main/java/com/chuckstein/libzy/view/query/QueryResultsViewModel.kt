package com.chuckstein.libzy.view.query

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.chuckstein.libzy.BuildConfig
import com.chuckstein.libzy.common.CombinedLiveData
import com.chuckstein.libzy.model.Query
import com.chuckstein.libzy.recommendation.RecommendationService
import com.chuckstein.libzy.repository.UserLibraryRepository
import com.chuckstein.libzy.spotify.remote.SpotifyAppRemoteService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findParameterByName

class QueryResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val spotifyAppRemoteService: SpotifyAppRemoteService
) : ViewModel() {

    companion object {
        private val TAG = QueryResultsViewModel::class.java.simpleName
    }

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

    // we should recalculate the recommendations from the user's library
    // whenever their query updates or their library updates
    private val recommendationInput = CombinedLiveData(queryLiveData, userLibraryRepository.libraryAlbums)

    val recommendedAlbums = recommendationInput.map { (query, libraryAlbums) ->
        recommendationService.recommendAlbums(query, libraryAlbums)
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
            Log.d(
                TAG, "title: ${album.title}\ngenres: ${album.genres}\n" +
                        "acousticness: ${album.audioFeatures.acousticness}\n" +
                        "danceability: ${album.audioFeatures.danceability}\n" +
                        "energy: ${album.audioFeatures.energy}\n" +
                        "instrumentalness: ${album.audioFeatures.instrumentalness}\n" +
                        "valence: ${album.audioFeatures.valence}\n" +
                        "longTermFavorite: ${album.familiarity.longTermFavorite}\n" +
                        "mediumTermFavorite: ${album.familiarity.mediumTermFavorite}\n" +
                        "shortTermFavorite: ${album.familiarity.shortTermFavorite}\n" +
                        "recentlyPlayed: ${album.familiarity.recentlyPlayed}\n" +
                        "lowFamiliarity: ${album.familiarity.isLowFamiliarity()}\n"
            )
        }
    }

}