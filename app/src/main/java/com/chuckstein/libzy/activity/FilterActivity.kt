package com.chuckstein.libzy.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.chuckstein.libzy.R
import com.chuckstein.libzy.extension.initializeBackground
import com.chuckstein.libzy.viewmodel.FilterViewModel
import kaaes.spotify.webapi.android.SpotifyApi
import kaaes.spotify.webapi.android.models.Artists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response


class FilterActivity : AppCompatActivity() {

    companion object {
        val TAG = FilterActivity::class.java.simpleName
    }

    private val model: FilterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)
        initializeBackground()

        model.genres.observe(this, Observer { genres ->
            // TODO
        })

        // TODO: move this to a repository
        CoroutineScope(IO).launch {
            getSavedGenres()
        }
    }

    // TODO: move this to a repository
    private fun getSavedGenres() {
        val api = SpotifyApi()
        val accessToken =
            getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
                .getString(
                    getString(R.string.spotify_access_token_key),
                    "cannot locate access token"
                ) // TODO: better default value? is it necessary to check first if it exists?
        api.setAccessToken(accessToken)
        val spotify = api.service
        val savedAlbums = spotify.mySavedAlbums.items // TODO: figure out how to change limit
        val genres = mutableSetOf<String>()
        val artistIds = mutableSetOf<String>()

        for (savedAlbum in savedAlbums) {
            genres.addAll(savedAlbum.album.genres)
            artistIds.addAll(savedAlbum.album.artists.map { artist -> artist.id })
        }

        // TODO: continue savedAlbums API calls until we've checked every album

        for (ids in artistIds.chunked(50)) { // TODO: make 50 a constant, like "SPOTIFY_API_ARG_LIMIT"
            spotify.getArtists(ids.joinToString(","), object : Callback<Artists> {
                override fun success(artistsObj: Artists, response: Response?) {
                    for (artist in artistsObj.artists) {
                        genres.addAll(artist.genres)
                    }
                    Log.d(TAG, "$genres"); // TODO: delete this line, check if this is final loop iteration, in which case fill UI
                }

                override fun failure(error: RetrofitError) {
                    // TODO: log and handle error
                }
            })
        }
    }

}
