package io.libzy.repository

import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.AudioFeatures
import io.libzy.common.percentageToFloat
import io.libzy.database.UserLibraryDatabase
import io.libzy.database.entity.DbAlbum
import io.libzy.database.entity.DbGenre
import io.libzy.database.entity.junction.AlbumGenreJunction
import io.libzy.database.tuple.AudioFeaturesTuple
import io.libzy.database.tuple.FamiliarityTuple
import io.libzy.spotify.api.SpotifyApiDelegator
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

    // TODO: should this initialization be done in a coroutine since it's using the db? why don't I get the warning that Room shouldn't be accessed from main thread?
    val libraryAlbums = database.albumDao.getAllAlbums()

    // TODO: ensure this task can run in background (ANSWER: not consistently, so probably has to be a @Service) -- and if it takes more than ~10 seconds make that obvious from UI (first library load probably needs a redesign if it takes a very long time)
    // TODO: for each request made here, think about how often I should be making it
    // TODO: if UI is stable enough to handle continuous updates, continuously flow/stream new library data by inserting each album and its genres as data is gathered?
    // TODO: save top artist data in either an artist entity or a "familiarity" DbAlbum field or a "<duration>_term_fav_artist" DbAlbum field
    //      - or maybe artists shouldn't be considered as factoring into favorite albums, because they could be listening to a different album by that artist
    // TODO: think about how I want to store/present data for recency and familiarity
    /**
     * Run a Spotify library sync by requesting the user's latest library data from the Spotify API,
     * converting that data to Libzy's schema, and caching it in the local database.
     *
     * @return the number of albums synced from the user's Spotify library
     */
    suspend fun syncLibraryData(): Int = withContext(Dispatchers.IO) {
        Timber.d("Fetching recently played tracks")
        val recentlyPlayedTracks = spotifyApi.getPlayHistory().map { it.track } // TODO: add an "after" time stamp so if they last played Spotify over a week ago it doesn't count as recently played?
        Timber.d("Fetching top tracks -- short term")
        val topTracksShortTerm = spotifyApi.getTopTracks(ClientPersonalizationApi.TimeRange.SHORT_TERM)
        Timber.d("Fetching top tracks -- medium term")
        val topTracksMediumTerm = spotifyApi.getTopTracks(ClientPersonalizationApi.TimeRange.MEDIUM_TERM)
        Timber.d("Fetching top tracks -- long term")
        val topTracksLongTerm = spotifyApi.getTopTracks(ClientPersonalizationApi.TimeRange.LONG_TERM)

        // TODO: determine whether it's useful to have top artists if we already have top track granularity
//            val topArtistsShortTerm = spotifyApi.getTopArtists(ClientPersonalizationApi.TimeRange.SHORT_TERM)
//            val topArtistsMediumTerm = spotifyApi.getTopArtists(ClientPersonalizationApi.TimeRange.MEDIUM_TERM)
//            val topArtistsLongTerm = spotifyApi.getTopArtists(ClientPersonalizationApi.TimeRange.LONG_TERM)

        Timber.d("Fetching saved albums")
        val albums = spotifyApi.getAllSavedAlbums().map { savedAlbum -> savedAlbum.album }

        val dbAlbums = albums.map { album ->
            toDbAlbum(
                album,
                recentlyPlayedTracks.map { it.id },
                topTracksShortTerm.map { it.id },
                topTracksMediumTerm.map { it.id },
                topTracksLongTerm.map { it.id }
            )
        }

        Timber.d("Scanning genres in saved albums")
        val dbGenres = mutableSetOf<DbGenre>()
        val albumGenreJunctions = mutableSetOf<AlbumGenreJunction>()
        fillGenreDataFromAlbums(dbGenres, albumGenreJunctions, albums)
        
        Timber.d("Saving library data in local database")
        database.runInTransaction { // TODO: ensure the nested transactions work well
            database.albumDao.replaceAll(dbAlbums)
            database.genreDao.replaceAll(dbGenres)
            database.albumGenreJunctionDao.replaceAll(albumGenreJunctions)
        }
        return@withContext albums.size
    }

    private suspend fun getAlbumAudioFeatures(album: Album): AudioFeaturesTuple {
        Timber.d("Fetching audio features for ${album.name}")
        val cachedAudioFeatures = withContext(Dispatchers.IO) { database.albumDao.getAudioFeatures(album.id) }
        if (cachedAudioFeatures != null) return cachedAudioFeatures

        val albumTrackIds = album.tracks.items.map { it.id }
        // TODO: log a warning if there's a null entry? also check to ensure all rate-limited requests eventually go though
        val audioFeaturesOfTracks = spotifyApi.getAudioFeaturesOfTracks(albumTrackIds).filterNotNull()
        if (audioFeaturesOfTracks.isEmpty()) return AudioFeaturesTuple(0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F)

        // TODO: check if SQLite can handle NaN case from average(), if there are somehow no audio features for an album? can that even happen?
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
    private suspend fun toDbAlbum(
        spotifyAlbum: Album,
        recentlyPlayedTrackIds: List<String>,
        topTrackIdsShortTerm: List<String>,
        topTrackIdsMediumTerm: List<String>,
        topTrackIdsLongTerm: List<String>
    ): DbAlbum {

        // TODO: determine whether an album counts as recent/favorite if ANY of its tracks are recent/favorite or AT LEAST HALF are recent/favorite
        fun albumTracksAreInList(album: Album, trackIds: List<String>) =
            album.tracks.items.any { trackIds.contains(it.id) }

        return DbAlbum(
            spotifyAlbum.id,
            spotifyAlbum.uri.uri,
            spotifyAlbum.name,
            spotifyAlbum.artists.joinToString(", ") { it.name },
            spotifyAlbum.images.getOrNull(0)?.url,
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
