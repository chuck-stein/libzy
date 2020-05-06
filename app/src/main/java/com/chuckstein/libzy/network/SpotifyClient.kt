package com.chuckstein.libzy.network

import android.util.Log
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyClientApiBuilder
import com.adamratzman.spotify.SpotifyUserAuthorizationBuilder
import com.adamratzman.spotify.models.Artist

// TODO: read up on android Services to see if I should delegate to a Service for extended API calls that will take time?
//       or find alternate best way? should it be a singleton? why are singletons bad again?
class SpotifyClient(accessToken: String) {

    private val api: SpotifyClientApi

    companion object {
        private val TAG = SpotifyClient::class.java.simpleName
        private const val API_ARG_LIMIT = 50
    }

    init {
        val authorization = SpotifyUserAuthorizationBuilder(tokenString = accessToken).build()
        api = SpotifyClientApiBuilder(authorization = authorization).build() // TODO: customize api options
    }

    // TODO: if we've previously gotten this info for the current user, only get new albums that have been saved since then, and append that to previous result
    fun loadSavedAlbumsGroupedByGenre(): Map<String, Set<String>> {
        // a map of genre names to album IDs associated with that genre
        val albumsGroupedByGenre = mutableMapOf<String, MutableSet<String>>()
        // a map of artist IDs to album IDs associated with that artist
        val albumsGroupedByArtist = mutableMapOf<String, MutableSet<String>>()
        val startTime = System.currentTimeMillis()

        val albums = api.library.getSavedAlbums().getAllItems().complete().map { savedAlbum -> savedAlbum.album }
        for (album in albums) {
            addToGrouping(album.id, album.genres, albumsGroupedByGenre)
            addToGrouping(album.id, album.artists.map { a -> a.id }, albumsGroupedByArtist)
        }

        val artistIdBatches = albumsGroupedByArtist.keys.chunked(API_ARG_LIMIT)
        for (artistIdBatch in artistIdBatches) {
            val artists = api.artists.getArtists(*artistIdBatch.toTypedArray()).complete()
            addArtistGenresToAlbums(artists, albumsGroupedByArtist, albumsGroupedByGenre)
        }

        val elapsedTime = System.currentTimeMillis() - startTime // TODO: use measureTimeMillis instaed
        Log.d(TAG, "Finished collecting album & genre data in $elapsedTime milliseconds.")
        return albumsGroupedByGenre
    }

    private fun addArtistGenresToAlbums(
        artists: List<Artist?>,
        albumsGroupedByArtist: MutableMap<String, MutableSet<String>>,
        albumsGroupedByGenre: MutableMap<String, MutableSet<String>>
    ) {
        for (artist in artists) {
            if (artist != null) {
                val albumsByThisArtist = albumsGroupedByArtist[artist.id]
                if (albumsByThisArtist != null) {
                    for (albumId in albumsByThisArtist) {
                        addToGrouping(albumId, artist.genres, albumsGroupedByGenre)
                    }
                }
            }
        }
    }

    private fun <T, S> addToGrouping(item: S, groups: Iterable<T>, grouping: MutableMap<T, MutableSet<S>>) {
        for (group in groups) {
            val groupItems = grouping[group]
            if (groupItems != null) {
                groupItems.add(item)
            } else {
                grouping[group] = mutableSetOf(item)
            }
        }
    }

}