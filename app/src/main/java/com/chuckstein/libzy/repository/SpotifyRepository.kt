package com.chuckstein.libzy.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyClientApiBuilder
import com.adamratzman.spotify.SpotifyUserAuthorizationBuilder
import com.chuckstein.libzy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: handle failed API calls?
class SpotifyRepository(context: Context) { // TODO: read up on android Services to see if I should delegate to a Service for extended API calls that will take time? or find alternate best way? should it be a singleton? why are singletons bad again?

    private val api: SpotifyClientApi

    companion object {
        private val TAG = SpotifyRepository::class.java.simpleName
        private const val API_ARG_LIMIT = 50
    }

    init {
        val sharedPref = context.getSharedPreferences(context.getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        val tokenKey = context.getString(R.string.spotify_access_token_key)
        val accessToken = sharedPref.getString(tokenKey, null)
        val authorization = SpotifyUserAuthorizationBuilder(tokenString = accessToken).build()
        api = SpotifyClientApiBuilder(authorization = authorization).build() // TODO: customize api options

//        sharedPref.registerOnSharedPreferenceChangeListener { prefs, key ->
//            if (key == tokenKey) {
//                // TODO: set API's access token to the new one
//            }
//        }
    }

    private fun <T, S> addToGrouping(
        item: S,
        groups: Iterable<T>,
        grouping: MutableMap<T, MutableSet<S>>
    ) {
        for (group in groups) {
            val groupItems = grouping[group]
            if (groupItems != null) {
                groupItems.add(item)
            } else {
                grouping[group] = mutableSetOf(item)
            }
        }
    }

    // TODO: if we've previously gotten this info for the current user, only get new albums that have been saved since then, and append that to previous result
    fun loadSavedAlbumsGroupedByGenre(resultLiveData: MutableLiveData<Map<String, Set<String>>>) {
        CoroutineScope(Dispatchers.IO).launch {
            // a map of genre names to album IDs associated with that genre
            val albumsGroupedByGenre = mutableMapOf<String, MutableSet<String>>()

            // a map of artist IDs to album IDs associated with that artist
            val albumsGroupedByArtist = mutableMapOf<String, MutableSet<String>>()

            var startTime = System.currentTimeMillis()
            val albums = api.library.getSavedAlbums().getAllItems().complete().map { savedAlbum -> savedAlbum.album }
            var elapsedTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Completed albums request in $elapsedTime milliseconds.")

            startTime = System.currentTimeMillis()
            for (album in albums) {
                addToGrouping(album.id, album.genres, albumsGroupedByGenre)
                addToGrouping(album.id, album.artists.map { a -> a.id }, albumsGroupedByArtist)
            }
            elapsedTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Completed album grouping in $elapsedTime milliseconds.")

            startTime = System.currentTimeMillis()
            val artistIdBatches = albumsGroupedByArtist.keys.chunked(API_ARG_LIMIT)
            for ((batchNum, artistIds) in artistIdBatches.withIndex()) {
                api.artists.getArtists(*artistIds.toTypedArray()).queue { artists ->
                    for (artist in artists) { // TODO: helper?
                        if (artist != null) {
                            val albumsByThisArtist = albumsGroupedByArtist[artist.id]
                            if (albumsByThisArtist != null) {
                                for (albumId in albumsByThisArtist) {
                                    addToGrouping(albumId, artist.genres, albumsGroupedByGenre)
                                }
                            }
                        }
                    }
                    if (batchNum == artistIdBatches.size - 1) { // TODO: check for off-by-one
                        elapsedTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Completed genre grouping in $elapsedTime milliseconds.")
                        resultLiveData.postValue(albumsGroupedByGenre)
                    }
                }
            }
        }
    }

}