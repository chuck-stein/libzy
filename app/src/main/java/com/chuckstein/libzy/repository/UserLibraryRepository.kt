package com.chuckstein.libzy.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.adamratzman.spotify.models.Album
import com.chuckstein.libzy.common.capitalizeAsHeading
import com.chuckstein.libzy.database.UserLibraryDatabase
import com.chuckstein.libzy.database.entity.DbAlbum
import com.chuckstein.libzy.database.entity.junction.AlbumGenreJunction
import com.chuckstein.libzy.database.entity.DbGenre
import com.chuckstein.libzy.spotify.api.SpotifyApiDelegator
import com.chuckstein.libzy.view.browseresults.data.AlbumResult
import com.chuckstein.libzy.view.browseresults.data.GenreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLibraryRepository @Inject constructor(
    private val database: UserLibraryDatabase,
    private val spotifyApi: SpotifyApiDelegator
) {

    // TODO: should this initialization be done in a coroutine since it's using the db? why don't I get the warning that Room shouldn't be accessed from main thread?
    val libraryGenres = Transformations.map(database.genreDao.getAllLibraryGenres()) { genresWithMetadata ->
        genresWithMetadata.map { it.name to it.numAssociatedAlbums }
            .toMap() // TODO: make the value of the map a GenreMetadata type, which contains more stuff like "is favorite", "recently listened", etc.
    }

    suspend fun getResultsFromGenreSelection(genres: List<String>): LiveData<List<GenreResult>> {
        return withContext(Dispatchers.IO) {
            Transformations.map(database.genreDao.getGenresWithAlbums(genres)) { genresWithAlbums ->
                genresWithAlbums.map { genreWithAlbum ->
                    GenreResult(
                        genreWithAlbum.genre.capitalizeAsHeading(), // TODO: should UI cleanup like this go in ViewModel instead? or better yet, since it's already done by the skeleton screen, don't do it here at all!
                        genreWithAlbum.albums.map { AlbumResult(it.title, it.artists, it.artworkUrl, it.spotifyUri) }
                    )
                }
            }
        }
    }


    suspend fun refreshLibraryData() {
        withContext(Dispatchers.IO) {
            val albums = spotifyApi.getAllSavedAlbums().map { savedAlbum -> savedAlbum.album }
            val dbAlbums = albums.map { toDbAlbum(it) }
            val dbGenres = mutableSetOf<DbGenre>()
            val albumGenreJunctions = mutableSetOf<AlbumGenreJunction>()
            fillGenreDataFromAlbums(dbGenres, albumGenreJunctions, albums)
            database.runInTransaction { // TODO: ensure the nested transactions work
                database.albumDao.replaceAll(dbAlbums)
                database.genreDao.replaceAll(dbGenres)
                database.albumGenreJunctionDao.replaceAll(albumGenreJunctions)
            }
        }
    }

    private suspend fun fillGenreDataFromAlbums(
        dbGenres: MutableSet<DbGenre>,
        albumGenreJunctions: MutableSet<AlbumGenreJunction>,
        albums: List<Album>
    ) { // TODO: ensure sets/keys/collisions/hashing work with this data class
        val albumsByArtists =
            mutableMapOf<String, MutableSet<String>>() // TODO: use existing junction data class if it will be in the final schema?
        for (album in albums) {
            for (genre in album.genres) {
                dbGenres.add(DbGenre(genre))
                albumGenreJunctions.add(AlbumGenreJunction(album.id, genre))
            }
            for (artistId in album.artists.map { it.id }) {
                albumsByArtists[artistId].let { albumsByArtist ->
                    if (albumsByArtist != null) albumsByArtist.add(album.id) // TODO: figure out how to do this syntax the way JetBrains wants me to...
                    else albumsByArtists[artistId] = mutableSetOf(album.id)
                }
            }
        }
        val artists = spotifyApi.getArtists(albumsByArtists.keys).filterNotNull()
        for (artist in artists) {
            albumsByArtists[artist.id]?.let { albumsByArtist ->
                for (albumId in albumsByArtist) {
                    for (genre in artist.genres) {
                        dbGenres.add(DbGenre(genre))
                        albumGenreJunctions.add(AlbumGenreJunction(albumId, genre))
                    }
                }
            }
        }
    }

    // TODO: move this to an extensions helper file? make it a function on List<Album>?
    private fun toDbAlbum(spotifyAlbum: Album) =
        DbAlbum(
            spotifyAlbum.id,
            spotifyAlbum.name,
            spotifyAlbum.artists.joinToString(", ") { it.name },
            spotifyAlbum.images.getOrNull(0)?.url,
            spotifyAlbum.uri.uri
        )

}