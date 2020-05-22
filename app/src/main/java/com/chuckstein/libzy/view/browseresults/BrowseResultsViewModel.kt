package com.chuckstein.libzy.view.browseresults

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamratzman.spotify.SpotifyException
import com.chuckstein.libzy.common.capitalizeAsHeading
import com.chuckstein.libzy.repository.UserLibraryRepository
import com.chuckstein.libzy.view.browseresults.data.GenreResult
import com.chuckstein.libzy.spotify.auth.SpotifyAuthException
import com.chuckstein.libzy.spotify.remote.SpotifyAppRemoteService
import com.chuckstein.libzy.view.browseresults.data.AlbumResult
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.IllegalArgumentException
import javax.inject.Inject

class BrowseResultsViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val spotifyAppRemoteService: SpotifyAppRemoteService
) : ViewModel() {

    companion object {
        private val TAG = BrowseResultsViewModel::class.java.simpleName
    }

    val spotifyPlayerState = spotifyAppRemoteService.playerState
    val spotifyPlayerContext = spotifyAppRemoteService.playerContext

    // TODO: abstract this (and its fragment Observer) to an abstract class or interface
    private val _receivedSpotifyNetworkError = MutableLiveData<Boolean>()
    val receivedSpotifyNetworkError: LiveData<Boolean>
        get() = _receivedSpotifyNetworkError

    init {
        viewModelScope.launch {
            try {
                userLibraryRepository.refreshLibraryData() // TODO: do I want to refresh here? or should it be a main activity/application thing? Check Udacity course for WorkManager stuff (buuuuut I probably can't do that cause my auth only lasts 60 minutes)
            } catch (e: Exception) {
                // TODO: abstract this (and its fragment Observer) to an abstract class or interface
                if (e is SpotifyException || e is SpotifyAuthException) {
                    Log.e(TAG, "Received a Spotify network error", e)
                    _receivedSpotifyNetworkError.value = true
                } else throw e
            }
        }
    }

    // TODO: this should only be album data -- genre name is already taken care of in skeleton screen (when updating data set, think about weird cases where number of albums might be different from skeleton screen)
    // TODO: repository result should not include view-specific stuff, but we should do a map transformation here into that format
    suspend fun getResults(selectedGenres: List<String>): LiveData<List<GenreResult>> =
        userLibraryRepository.getResultsFromGenreSelection(selectedGenres)

    fun createSkeletonScreenResults(
        selectedGenres: Array<String>,
        numAlbumsPerSelectedGenre: IntArray
    ): List<GenreResult> {
        if (selectedGenres.size != numAlbumsPerSelectedGenre.size) {
            throw IllegalArgumentException(
                "The given arrays for selected genres and album count per selected genre must be of the same size"
            )
        }
        val skeletonScreenGenres = mutableListOf<GenreResult>()
        for ((index, genre) in selectedGenres.withIndex()) {
            val skeletonScreenAlbums = List(numAlbumsPerSelectedGenre[index]) {
                AlbumResult("Fetching album data", "Please wait...", isPlaceholder = true)
                // TODO: instead of text placeholders, make text an empty string, add a shimmer, and make background non-transparent (if shimmerframelayout doesn't do that already)
            }
            skeletonScreenGenres.add(GenreResult(genre.capitalizeAsHeading(), skeletonScreenAlbums))
        }
        return skeletonScreenGenres
    }

    fun connectSpotifyAppRemote(onFailure: () -> Unit) {
        spotifyAppRemoteService.connect(onFailure)
    }

    fun disconnectSpotifyAppRemote() {
        spotifyAppRemoteService.disconnect()
    }

    fun playAlbum(spotifyUri: String) {
        spotifyAppRemoteService.playAlbum(spotifyUri)
    }

}