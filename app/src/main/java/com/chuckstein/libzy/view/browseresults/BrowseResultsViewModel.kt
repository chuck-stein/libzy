package com.chuckstein.libzy.view.browseresults

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamratzman.spotify.SpotifyException
import com.chuckstein.libzy.view.browseresults.data.GenreResult
import com.chuckstein.libzy.network.SpotifyClient
import com.chuckstein.libzy.network.auth.SpotifyAuthException
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class BrowseResultsViewModel @Inject constructor(private val spotifyClient: SpotifyClient) : ViewModel() {

    companion object {
        private val TAG = BrowseResultsViewModel::class.java.simpleName
    }

    private val _genreResults = MutableLiveData<List<GenreResult>>()
    val genreResults: LiveData<List<GenreResult>>
        get() = _genreResults

    private var requestedResults = false

    // TODO: abstract this (and its fragment Observer) to an abstract class or interface
    private val _receivedSpotifyNetworkError = MutableLiveData<Boolean>()
    val receivedSpotifyNetworkError: LiveData<Boolean>
        get() = _receivedSpotifyNetworkError

    fun fetchResults(selectedGenres: Array<String>) {
        if (!requestedResults) {
            viewModelScope.launch {
                try {
                    _genreResults.value = spotifyClient.loadResultsFromGenreSelection(selectedGenres)
                } catch (e: Exception) {
                    // TODO: abstract this (and its fragment Observer) to an abstract class or interface
                    if (e is SpotifyException || e is SpotifyAuthException) {
                        Log.e(TAG, "Received a Spotify network error", e)
                        _receivedSpotifyNetworkError.value = true
                    } else throw e
                }
            }
            requestedResults = true
        }
    }

    fun playAlbum(spotifyUri: String) {
        // TODO (delegate to a SpotifyRemoteService?)
    }

}