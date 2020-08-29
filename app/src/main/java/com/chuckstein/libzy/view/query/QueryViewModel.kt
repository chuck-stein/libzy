package com.chuckstein.libzy.view.query

import android.util.Log
import androidx.lifecycle.*
import com.adamratzman.spotify.SpotifyException
import com.chuckstein.libzy.repository.UserLibraryRepository
import com.chuckstein.libzy.spotify.auth.SpotifyAuthException
import kotlinx.coroutines.launch
import javax.inject.Inject

class QueryViewModel @Inject constructor(private val userLibraryRepository: UserLibraryRepository) :
    ViewModel() {

    companion object {
        private val TAG = QueryViewModel::class.java.simpleName
    }

    enum class Familiarity {
        CURRENT_FAVORITE, RELIABLE_CLASSIC, UNDERAPPRECIATED_GEM
    }

    // query parameters
    var familiarity: Familiarity? = null
    var instrumental: Boolean? = null
    var acousticness: Float? = null
    var valence: Float? = null
    var energy: Float? = null
    var danceability: Float? = null
    var genres: Set<String>? = null

    // TODO: abstract this (and its fragment Observer) to an abstract class or interface
    private val _receivedSpotifyNetworkError = MutableLiveData<Boolean>()
    val receivedSpotifyNetworkError: LiveData<Boolean>
        get() = _receivedSpotifyNetworkError

    init {
        viewModelScope.launch {
            try {
                userLibraryRepository.refreshLibraryData() // TODO: do I want to refresh here? or should it be a main activity/application thing? Check Udacity course for WorkManager stuff
            } catch (e: Exception) {
                // TODO: abstract this (and its fragment Observer) to an abstract class or interface
                if (e is SpotifyException || e is SpotifyAuthException) {
                    Log.e(TAG, "Received a Spotify network error", e)
                    _receivedSpotifyNetworkError.value = true
                } else throw e
            }
        }
    }

    fun submitQuery() {
        Log.d(
            TAG, "submitQuery: familiarity=$familiarity, instrumental=$instrumental, acousticness=$acousticness, " +
                    "valence=$valence, energy=$energy, danceability=$danceability, genres=$genres"
        )
    }

    // TODO: replace this placeholder implementation with a call to a getGenreSuggestions() function in a RecommendationService,
    //       that takes in userLibraryRepository.libraryGenres as input and gives a list of genres applicable to results of
    //       questions answered so far, sorted by number of such results for each genre)
    //       (think about how to handle the libraryGenres is null case -- when would it be null?
    //        maybe show a loading screen if it will be not null soon, or just skip the genres question)
    fun getGenreSuggestions(): LiveData<List<String>> =
        Transformations.map(userLibraryRepository.libraryGenres) { libraryGenres ->
            libraryGenres.keys.sortedByDescending { libraryGenres[it] }.take(50)
        }

    fun updateSelectedGenres(genreOptions: List<String>) {
        genres.let { previousGenres ->
            if (previousGenres != null) genres = genreOptions.filter { previousGenres.contains(it) }.toSet()
        }
    }

}