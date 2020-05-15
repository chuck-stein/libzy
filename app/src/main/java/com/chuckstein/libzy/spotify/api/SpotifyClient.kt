package com.chuckstein.libzy.spotify.api

import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.SimpleArtist
import com.adamratzman.spotify.models.SpotifyImage
import com.chuckstein.libzy.common.capitalizeAsHeading
import com.chuckstein.libzy.view.browseresults.data.AlbumResult
import com.chuckstein.libzy.view.browseresults.data.GenreResult
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton

// TODO: read up on android Services to see if I should delegate to a Service for extended API calls that will take time? or find alternate best way?
@Singleton
class SpotifyClient @Inject constructor(private val api: SpotifyApiDelegator) {

    // TODO: remove this quick hack in favor of SQLite caching (unless this makes sense for faster in-memory caching...? but still would use Room as backup)
    private var cachedAlbumsByGenre: Map<String, Set<String>>? = null
    suspend fun loadResultsFromGenreSelection(selectedGenres: Array<String>): List<GenreResult> {
        cachedAlbumsByGenre.let { cachedAlbumsByGenre ->
            if (cachedAlbumsByGenre == null) throw IllegalStateException("No cached library data!") // TODO: if client cache does end up being final, fix this
            val results = mutableListOf<GenreResult>()
            for (genre in selectedGenres) {
                val albumIds = cachedAlbumsByGenre[genre]
                    ?: throw IllegalArgumentException("The given genre $genre is not in the user's library!")
                val albums = api.getAlbums(albumIds)
                // TODO: log and/or error if an album is null, meaning we requested an invalid ID? instead of just filtering?
                val albumResults = albums.filterNotNull().map { toAlbumResult(it) }
                results.add(GenreResult(genre.capitalizeAsHeading(), albumResults))  // TODO: capitalization should go in ViewModel as a LiveData transformation map
            }
            return results
        }
    }

    // TODO: should these all be local functions?

    private fun toAlbumResult(album: Album) =
        AlbumResult(album.name, artistsToString(album.artists), getArtworkUri(album.images), album.uri.uri)

    // TODO: maybe don't always use the largest image if I run into performance issues?
    private fun getArtworkUri(albumImages: List<SpotifyImage>) = albumImages.getOrNull(0)?.url

    // TODO: this should go in viewmodel
    private fun artistsToString(artists: List<SimpleArtist>) = artists.joinToString(", ") { it.name }


    // TODO: if we've previously gotten this info for the current user, only get new albums that have been saved since then, and append that to previous result
    suspend fun loadSavedAlbumsGroupedByGenre(): Map<String, Set<String>> {
        // a map of genre names to album IDs associated with that genre
        val albumsGroupedByGenre = mutableMapOf<String, MutableSet<String>>()
        // a map of artist IDs to album IDs associated with that artist
        val albumsGroupedByArtist = mutableMapOf<String, MutableSet<String>>()

        val albums = api.getAllSavedAlbums().map { savedAlbum -> savedAlbum.album }
        for (album in albums) {
            addToGrouping(album.id, album.genres, albumsGroupedByGenre)
            addToGrouping(album.id, album.artists.map { a -> a.id }, albumsGroupedByArtist)
        }

        val artists = api.getArtists(albumsGroupedByArtist.keys)
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

        cachedAlbumsByGenre = albumsGroupedByGenre // TODO: delete after SQLite refactor
        return albumsGroupedByGenre
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