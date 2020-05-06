package com.chuckstein.libzy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chuckstein.libzy.network.SpotifyClient
import com.chuckstein.libzy.network.SpotifyClientFactory
import kotlinx.coroutines.*

// TODO: refactor this to only contain list of genres, as that's all the View needs, and move the genre-album grouping to a deeper architecture layer
// TODO: override onCleared and tell SpotifyClientOld to cancel Spotify requests (maybe implement this using an Rx library and pass an observable in to loadSavedAlbumsGroupedByGenre which will get subscribed to cancel coroutines if onCleared pushes an event?)
//          (or if SpotifyClientOld ends up as a singleton, don't do this and instead keep the data cached somewhere to be checked next time the request is made)
class SelectGenresViewModel(applicationContext: Application) : AndroidViewModel(applicationContext) {

    companion object {
        private val TAG = SelectGenresViewModel::class.java.simpleName
    }

    private lateinit var spotifyClient: SpotifyClient

    private val _loadingShouldBegin = MutableLiveData<Boolean>()
    val loadingShouldBegin: LiveData<Boolean>
        get() = _loadingShouldBegin

    private val _loadingShouldEnd = MutableLiveData<Boolean>()
    val loadingShouldEnd: LiveData<Boolean>
        get() = _loadingShouldEnd

    private val _albumsGroupedByGenre = MutableLiveData<Map<String, Set<String>>>()
    val albumsGroupedByGenre: LiveData<Map<String, Set<String>>>
        get() = _albumsGroupedByGenre

    private val selectGenresViewModelJob = Job()

    init {
        _loadingShouldBegin.value = false
        _loadingShouldEnd.value = false
        CoroutineScope(Dispatchers.Main + selectGenresViewModelJob).launch {
            spotifyClient = SpotifyClientFactory(applicationContext).getClient() // TODO: make SpotifyClientFactory an injected dependency
            _loadingShouldBegin.value = true
            _albumsGroupedByGenre.value = loadSavedAlbumsGroupedByGenre()
            _loadingShouldEnd.value = true
        }
    }

    private suspend fun loadSavedAlbumsGroupedByGenre(): Map<String, Set<String>> {
        return withContext(Dispatchers.IO) {
            spotifyClient.loadSavedAlbumsGroupedByGenre()
        }
    }

    fun onLoadingStarted() {
        _loadingShouldBegin.value = false
    }

    fun onLoadingEnded() {
        _loadingShouldEnd.value = false
    }

    override fun onCleared() {
        super.onCleared()
        selectGenresViewModelJob.cancel()
    }

}