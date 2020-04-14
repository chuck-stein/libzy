package com.chuckstein.libzy.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SpotifyRepository {

    companion object {
        private val TAG = SpotifyRepository::class.java.simpleName
    }

    fun getLibraryGenres(): LiveData<Set<String>> {
        // TODO: actual implementation
        return MutableLiveData(emptySet())
    }

}