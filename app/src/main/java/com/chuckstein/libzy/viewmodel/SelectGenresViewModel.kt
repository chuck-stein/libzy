package com.chuckstein.libzy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.chuckstein.libzy.model.repository.SpotifyRepository

class SelectGenresViewModel(application: Application) : AndroidViewModel(application) {
    private val spotifyRepository = SpotifyRepository(application)
    val albumsGroupedByGenre = MutableLiveData<Map<String, Set<String>>>()

    init {
        spotifyRepository.loadSavedAlbumsGroupedByGenre(albumsGroupedByGenre)
    }
}