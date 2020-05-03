package com.chuckstein.libzy.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chuckstein.libzy.model.repository.SpotifyRepository
import kotlinx.coroutines.*

// TODO: refactor this to only contain list of genres, as that's all the View needs, and move the genre-album grouping to a deeper architecture layer
// TODO: override onCleared and tell SpotifyRepository to cancel Spotify requests (maybe implement this using an Rx library and pass an observable in to loadSavedAlbumsGroupedByGenre which will get subscribed to cancel coroutines if onCleared pushes an event?)
//          (or if SpotifyRepository ends up as a singleton, don't do this and instead keep the data cached somewhere to be checked next time the request is made)
class SelectGenresViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val TAG = SelectGenresViewModel::class.java.simpleName
    }

    private val spotifyRepository = SpotifyRepository(application)

    private val _newGenreDataReady = MutableLiveData<Boolean>()
    val newGenreDataReady: LiveData<Boolean>
        get() = _newGenreDataReady

    private val _albumsGroupedByGenre = MutableLiveData<Map<String, Set<String>>>()
    val albumsGroupedByGenre: LiveData<Map<String, Set<String>>>
        get() = _albumsGroupedByGenre

    private val loadSavedAlbumsGroupedByGenreJob = Job()

    init {
        _newGenreDataReady.value = false
        CoroutineScope(Dispatchers.Main + loadSavedAlbumsGroupedByGenreJob).launch {
            _albumsGroupedByGenre.value = loadSavedAlbumsGroupedByGenre()
            _newGenreDataReady.value = true
        }
    }

    private suspend fun loadSavedAlbumsGroupedByGenre(): Map<String, Set<String>> {
        return withContext(Dispatchers.IO) {
            spotifyRepository.loadSavedAlbumsGroupedByGenre()
        }
    }

    fun onLoadingEnded() {
        _newGenreDataReady.value = false
    }

    override fun onCleared() {
        super.onCleared()
        loadSavedAlbumsGroupedByGenreJob.cancel()
    }

}