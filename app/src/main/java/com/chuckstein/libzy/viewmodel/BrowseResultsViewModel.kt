package com.chuckstein.libzy.viewmodel

import android.annotation.SuppressLint
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
        _genreResults.value = selectedGenres.map { genre ->
            GenreData(
                genre.capitalizeEachWord(), listOf(
                    AlbumData("Crack the Skye", "Mastodon", Uri.EMPTY,""),
                    AlbumData("AEnima", "Tool", Uri.EMPTY,""),
                    AlbumData("Dopethrone", "Electric Wizard", Uri.EMPTY,""),
                    AlbumData("Baroness", "Purple", Uri.EMPTY,"")
                )
            )
        }
        /*
        _genreResults.value = listOf(
            GenreData(
                "Metal", listOf(
                    AlbumData("Crack the Skye", "Mastodon", Uri.EMPTY,""),
                    AlbumData("AEnima", "Tool", Uri.EMPTY,""),
                    AlbumData("Dopethrone", "Electric Wizard", Uri.EMPTY,""),
                    AlbumData("Baroness", "Purple", Uri.EMPTY,"")
                )
            ),
            GenreData(
                "Noise Rock", listOf(
                    AlbumData("Feelin Kinda Free", "The Drones", Uri.EMPTY,""),
                    AlbumData("You Won't Get What You Want", "Daughters", Uri.EMPTY,"")
                )
            ),
            GenreData(
                "Emo", listOf(
                    AlbumData("American Football", "American Football", Uri.EMPTY,""),
                    AlbumData("The Devil And God Are Raging Inside Me", "Brand New", Uri.EMPTY,""),
                    AlbumData("The Black Parade", "My Chemical Romance", Uri.EMPTY,"")
                )
            ),
            GenreData(
                "Metal 2", listOf(
                    AlbumData("Crack the Skye", "Mastodon", Uri.EMPTY,""),
                    AlbumData("AEnima", "Tool", Uri.EMPTY,""),
                    AlbumData("Dopethrone", "Electric Wizard", Uri.EMPTY,"")
                )
            ),
            GenreData(
                "Noise Rock 2", listOf(
                    AlbumData("Feelin Kinda Free", "The Drones", Uri.EMPTY,""),
                    AlbumData("You Won't Get What You Want", "Daughters", Uri.EMPTY,"")
                )
            ),
            GenreData(
                "Emo 2", listOf(
                    AlbumData("American Football", "American Football", Uri.EMPTY,""),
                    AlbumData("The Devil And God Are Raging Inside Me", "Brand New", Uri.EMPTY,""),
                    AlbumData("The Black Parade", "My Chemical Romance", Uri.EMPTY,"")
                )
            ),
            GenreData(
                "Metal 3", listOf(
                    AlbumData("Crack the Skye", "Mastodon", Uri.EMPTY,""),
                    AlbumData("AEnima", "Tool", Uri.EMPTY,""),
                    AlbumData("Dopethrone", "Electric Wizard", Uri.EMPTY,"")
                )
            ),
            GenreData(
                "Noise Rock 3", listOf(
                    AlbumData("Feelin Kinda Free", "The Drones", Uri.EMPTY,""),
                    AlbumData("You Won't Get What You Want", "Daughters", Uri.EMPTY,"")
                )
            ),
            GenreData(
                "Emo 3", listOf(
                    AlbumData("American Football", "American Football", Uri.EMPTY,""),
                    AlbumData("The Devil And God Are Raging Inside Me", "Brand New", Uri.EMPTY,""),
                    AlbumData("The Black Parade", "My Chemical Romance", Uri.EMPTY,"")
                )
            )

        )
         */
    }

    fun playAlbum(spotifyUri: String) {
        // TODO
    }

    // TODO: break this out into Util singleton if further need arises
    @SuppressLint("DefaultLocale")
    private fun String.capitalizeEachWord() =
        split(" ").joinToString(" ") { it.capitalize() }

}