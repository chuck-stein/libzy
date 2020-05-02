package com.chuckstein.libzy.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.chuckstein.libzy.viewmodel.data.AlbumData
import com.chuckstein.libzy.viewmodel.data.GenreData

class BrowseResultsViewModel(selectedGenres: Array<String>) : ViewModel() {

    private val _genreResults = MutableLiveData<List<GenreData>>()
    val genreResults: LiveData<List<GenreData>>
        get() = _genreResults

    init {
        // TODO: delete this dummy data, make actual request for full album data for each selected genre
        _genreResults.value = listOf(
            GenreData(
                "Metal", listOf(
                    AlbumData(Uri.EMPTY, "Crack the Skye", "Mastodon"),
                    AlbumData(Uri.EMPTY, "AEnima", "Tool"),
                    AlbumData(Uri.EMPTY, "Dopethrone", "Electric Wizard"),
                    AlbumData(Uri.EMPTY, "Baroness", "Purple")
                )
            ),
            GenreData(
                "Noise Rock", listOf(
                    AlbumData(Uri.EMPTY, "Feelin Kinda Free", "The Drones"),
                    AlbumData(Uri.EMPTY, "You Won't Get What You Want", "Daughters")
                )
            ),
            GenreData(
                "Emo", listOf(
                    AlbumData(Uri.EMPTY, "American Football", "American Football"),
                    AlbumData(Uri.EMPTY, "The Devil And God Are Raging Inside Me", "Brand New"),
                    AlbumData(Uri.EMPTY, "The Black Parade", "My Chemical Romance")
                )
            ),
            GenreData(
                "Metal 2", listOf(
                    AlbumData(Uri.EMPTY, "Crack the Skye", "Mastodon"),
                    AlbumData(Uri.EMPTY, "AEnima", "Tool"),
                    AlbumData(Uri.EMPTY, "Dopethrone", "Electric Wizard")
                )
            ),
            GenreData(
                "Noise Rock 2", listOf(
                    AlbumData(Uri.EMPTY, "Feelin Kinda Free", "The Drones"),
                    AlbumData(Uri.EMPTY, "You Won't Get What You Want", "Daughters")
                )
            ),
            GenreData(
                "Emo 2", listOf(
                    AlbumData(Uri.EMPTY, "American Football", "American Football"),
                    AlbumData(Uri.EMPTY, "The Devil And God Are Raging Inside Me", "Brand New"),
                    AlbumData(Uri.EMPTY, "The Black Parade", "My Chemical Romance")
                )
            ),
            GenreData(
                "Metal 3", listOf(
                    AlbumData(Uri.EMPTY, "Crack the Skye", "Mastodon"),
                    AlbumData(Uri.EMPTY, "AEnima", "Tool"),
                    AlbumData(Uri.EMPTY, "Dopethrone", "Electric Wizard")
                )
            ),
            GenreData(
                "Noise Rock 3", listOf(
                    AlbumData(Uri.EMPTY, "Feelin Kinda Free", "The Drones"),
                    AlbumData(Uri.EMPTY, "You Won't Get What You Want", "Daughters")
                )
            ),
            GenreData(
                "Emo 3", listOf(
                    AlbumData(Uri.EMPTY, "American Football", "American Football"),
                    AlbumData(Uri.EMPTY, "The Devil And God Are Raging Inside Me", "Brand New"),
                    AlbumData(Uri.EMPTY, "The Black Parade", "My Chemical Romance")
                )
            )

        )
    }

}