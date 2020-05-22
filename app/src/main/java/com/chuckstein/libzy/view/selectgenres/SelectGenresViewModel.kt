package com.chuckstein.libzy.view.selectgenres

import android.util.Log
import androidx.lifecycle.*
import com.adamratzman.spotify.SpotifyException
import com.chuckstein.libzy.repository.UserLibraryRepository
import com.chuckstein.libzy.spotify.auth.SpotifyAuthException
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class SelectGenresViewModel @Inject constructor(private val userLibraryRepository: UserLibraryRepository) :
    ViewModel() {

    companion object {
        private val TAG = SelectGenresViewModel::class.java.simpleName
    }

    val genreOptions = userLibraryRepository.libraryGenres

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

}