package io.libzy.repository

import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.AudioFeatures
import io.libzy.database.UserLibraryDatabase
import io.libzy.database.entity.DbAlbum
import io.libzy.database.entity.DbGenre
import io.libzy.database.entity.junction.AlbumGenreJunction
import io.libzy.database.tuple.AudioFeaturesTuple
import io.libzy.database.tuple.FamiliarityTuple
import io.libzy.spotify.api.SpotifyApiDelegator
import io.libzy.util.percentageToFloat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLibraryRepository @Inject constructor(
    private val database: UserLibraryDatabase,
    private val spotifyApi: SpotifyApiDelegator
) {

    val albums = database.albumDao.getAllAlbums()

    /**
     * Run a Spotify library sync by requesting the user's latest library data from the Spotify API,
     * converting that data to Libzy's schema, and caching it in the local database.
     *
     * @return the number of albums synced from the user's Spotify library
     */
    suspend fun syncLibraryData(): Int = withContext(Dispatchers.IO) {
        Timber.v("Fetching recently played tracks")
        val recentlyPlayedTracks = spotifyApi.fetchPlayHistory().map { it.track }
        Timber.v("Fetching top tracks -- short term")
        val topTracksShortTerm = spotifyApi.fetchTopTracks(ClientPersonalizationApi.TimeRange.SHORT_TERM)
        Timber.v("Fetching top tracks -- medium term")
        val topTracksMediumTerm = spotifyApi.fetchTopTracks(ClientPersonalizationApi.TimeRange.MEDIUM_TERM)
        Timber.v("Fetching top tracks -- long term")
        val topTracksLongTerm = spotifyApi.fetchTopTracks(ClientPersonalizationApi.TimeRange.LONG_TERM)

        Timber.v("Fetching saved albums")
        val albums = spotifyApi.fetchAllSavedAlbums().map { savedAlbum -> savedAlbum.album }

        val dbAlbums = albums.map { album ->
            toDbAlbum(
                album,
                recentlyPlayedTracks.map { it.id },
                topTracksShortTerm.map { it.id },
                topTracksMediumTerm.map { it.id },
                topTracksLongTerm.map { it.id }
            )
        }

        Timber.v("Scanning genres in saved albums")
        val dbGenres = mutableSetOf<DbGenre>()
        val albumGenreJunctions = mutableSetOf<AlbumGenreJunction>()
        fillGenreDataFromAlbums(dbGenres, albumGenreJunctions, albums)
        
        Timber.v("Saving library data in local database")
        database.runInTransaction { // TODO: ensure nested transactions work as expected
            database.albumDao.replaceAll(dbAlbums)
            database.genreDao.replaceAll(dbGenres)
            database.albumGenreJunctionDao.replaceAll(albumGenreJunctions)
        }
        return@withContext albums.size
    }

    private suspend fun getAlbumAudioFeatures(album: Album): AudioFeaturesTuple {
        Timber.v("Fetching audio features for ${album.name}")
        val cachedAudioFeatures = withContext(Dispatchers.IO) { database.albumDao.getAudioFeatures(album.id) }
        if (cachedAudioFeatures != null) return cachedAudioFeatures

        val albumTrackIds = album.tracks.items.map { it.id }
        val audioFeaturesOfTracks = spotifyApi.fetchAudioFeaturesOfTracks(albumTrackIds).filterNotNull()
        if (audioFeaturesOfTracks.isEmpty()) return AudioFeaturesTuple(0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F)

        // TODO: handle NaN case if averaging an empty list
        fun findAudioFeatureAverage(getSpecificAudioFeature: (AudioFeatures) -> Float): Float =
            audioFeaturesOfTracks.map(getSpecificAudioFeature).average().toFloat()

        return AudioFeaturesTuple(
            findAudioFeatureAverage { it.valence },
            findAudioFeatureAverage { it.acousticness },
            findAudioFeatureAverage { it.instrumentalness },
            findAudioFeatureAverage { it.energy },
            findAudioFeatureAverage { it.danceability },
            findAudioFeatureAverage { it.liveness }
        )
    }

    private suspend fun fillGenreDataFromAlbums(
        dbGenres: MutableSet<DbGenre>,
        albumGenreJunctions: MutableSet<AlbumGenreJunction>,
        albums: List<Album>
    ) {
        val albumsByArtists = mutableMapOf<String, MutableSet<String>>()
        for (album in albums) {
            for (genre in album.genres) {
                dbGenres.add(DbGenre(genre))
                albumGenreJunctions.add(AlbumGenreJunction(album.id, genre))
            }
            for (artistId in album.artists.map { it.id }) {
                albumsByArtists.getOrPut(artistId, ::mutableSetOf).add(album.id)
            }
        }
        val artists = spotifyApi.fetchArtists(albumsByArtists.keys).filterNotNull()
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

    private suspend fun toDbAlbum(
        spotifyAlbum: Album,
        recentlyPlayedTrackIds: List<String>,
        topTrackIdsShortTerm: List<String>,
        topTrackIdsMediumTerm: List<String>,
        topTrackIdsLongTerm: List<String>
    ): DbAlbum {

        fun albumTracksAreInList(album: Album, trackIds: List<String>) =
            album.tracks.items.any { trackIds.contains(it.id) }

        return DbAlbum(
            spotifyAlbum.id,
            spotifyAlbum.uri.uri,
            spotifyAlbum.name,
            spotifyAlbum.artists.joinToString(", ") { it.name },
            spotifyAlbum.images.firstOrNull()?.url,
            spotifyAlbum.releaseDate.year,
            percentageToFloat(spotifyAlbum.popularity),
            getAlbumAudioFeatures(spotifyAlbum),
            FamiliarityTuple(
                albumTracksAreInList(spotifyAlbum, recentlyPlayedTrackIds),
                albumTracksAreInList(spotifyAlbum, topTrackIdsShortTerm),
                albumTracksAreInList(spotifyAlbum, topTrackIdsMediumTerm),
                albumTracksAreInList(spotifyAlbum, topTrackIdsLongTerm)
            )
        )
    }

    suspend fun getAlbumFromUri(spotifyUri: String) = database.albumDao.getAlbumFromUri(spotifyUri)

}
