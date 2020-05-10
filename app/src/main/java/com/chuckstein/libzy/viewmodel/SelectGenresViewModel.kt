package com.chuckstein.libzy.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adamratzman.spotify.SpotifyException
import com.chuckstein.libzy.network.SpotifyClient
import com.chuckstein.libzy.network.auth.SpotifyAuthException
import kotlinx.coroutines.launch
import java.lang.Exception

// TODO: refactor this to only contain list of genres, as that's all the View needs, and move the genre-album grouping to a deeper architecture layer
class SelectGenresViewModel(applicationContext: Application) : AndroidViewModel(applicationContext) {

    companion object {
        private val TAG = SelectGenresViewModel::class.java.simpleName
    }

    private val spotifyClient = SpotifyClient(applicationContext)

    private val _genreOptions = MutableLiveData<Map<String, Set<String>>>()
    val genreOptions: LiveData<Map<String, Set<String>>>
        get() = _genreOptions

    // TODO: abstract this (and its fragment Observer) to an abstract class or interface
    private val _receivedSpotifyNetworkError = MutableLiveData<Boolean>()
    val receivedSpotifyNetworkError: LiveData<Boolean>
        get() = _receivedSpotifyNetworkError

    init {
        viewModelScope.launch {
            try {
                _genreOptions.value = spotifyClient.loadSavedAlbumsGroupedByGenre()
            } catch (e: Exception) {
                // TODO: abstract this (and its fragment Observer) to an abstract class or interface
                if (e is SpotifyException || e is SpotifyAuthException) {
                    Log.e(TAG, "Received a Spotify network error", e)
                    _receivedSpotifyNetworkError.value = true
                } else throw e
            }
        }
    }

}