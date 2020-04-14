package com.chuckstein.libzy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.chuckstein.libzy.repository.SpotifyRepository

class FilterViewModel(
    spotifyRepository: SpotifyRepository = SpotifyRepository() // TODO: use dependency injection
) : ViewModel() {
    val genres: LiveData<Set<String>> = spotifyRepository.getLibraryGenres()
}