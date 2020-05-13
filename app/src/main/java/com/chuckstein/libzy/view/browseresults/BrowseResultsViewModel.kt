package com.chuckstein.libzy.view.browseresults

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.chuckstein.libzy.view.browseresults.data.AlbumData
import com.chuckstein.libzy.view.browseresults.data.GenreData
import com.chuckstein.libzy.common.capitalizeAsHeading
import com.chuckstein.libzy.network.SpotifyClient
import javax.inject.Inject

class BrowseResultsViewModel @Inject constructor(private val spotifyClient: SpotifyClient) : ViewModel() {

    private val _genreResults = MutableLiveData<List<GenreData>>()
    val genreResults: LiveData<List<GenreData>>
        get() = _genreResults

    private var requestedResults = false

    fun fetchResults(selectedGenres: Array<String>) {
        if (!requestedResults) {
            // TODO: delete this dummy data, make actual request for full album data for each selected genre
            _genreResults.value = selectedGenres.map { genre ->
                GenreData(
                    genre.capitalizeAsHeading(), listOf(
                        AlbumData("Crack the Skye", "Mastodon", Uri.EMPTY, ""),
                        AlbumData("AEnima", "Tool", Uri.EMPTY, ""),
                        AlbumData("Dopethrone", "Electric Wizard", Uri.EMPTY, ""),
                        AlbumData("Baroness", "Purple", Uri.EMPTY, "")
                    )
                )
            }

            requestedResults = true
        }
    }

    fun playAlbum(spotifyUri: String) {
        // TODO (delegate to a SpotifyRemoteService?)
    }

}