package com.chuckstein.libzy.view.connect

import androidx.lifecycle.ViewModel
import com.chuckstein.libzy.repository.UserLibraryRepository
import javax.inject.Inject

class ConnectSpotifyViewModel @Inject constructor(private val userLibraryRepository: UserLibraryRepository) :
    ViewModel() {

    suspend fun scanLibrary() {
        userLibraryRepository.refreshLibraryData()
    }

}