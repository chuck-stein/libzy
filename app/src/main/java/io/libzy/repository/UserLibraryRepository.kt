package io.libzy.repository

import androidx.room.withTransaction
import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi.TimeRange
import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi.TimeRange.LongTerm
import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi.TimeRange.MediumTerm
import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi.TimeRange.ShortTerm
import com.adamratzman.spotify.endpoints.client.LibraryType
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.AudioFeatures
import com.adamratzman.spotify.models.Track
import io.libzy.domain.artworkUrl
import io.libzy.persistence.database.UserLibraryDatabase
import io.libzy.persistence.database.entity.DbAlbum
import io.libzy.persistence.database.entity.DbGenre
import io.libzy.persistence.database.entity.DbTopTrack
import io.libzy.persistence.database.entity.junction.AlbumGenreJunction
import io.libzy.persistence.database.tuple.AudioFeaturesTuple
import io.libzy.persistence.database.tuple.FamiliarityTuple
import io.libzy.spotify.api.SpotifyApiDelegator
import io.libzy.util.percentageToFloat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLibraryRepository @Inject constructor(
    private val database: UserLibraryDatabase,
    private val spotifyApi: SpotifyApiDelegator,
    private val coroutineScope: CoroutineScope
) {
    val albums = database.albumDao.getAllAlbums()
    val genres = database.albumGenreJunctionDao.getAllGenresSortedByNumAssociatedAlbums()
    val topTracksLongTerm = database.topTrackDao.getLongTermTopTracks()
    val topTracksMediumTerm = database.topTrackDao.getMediumTermTopTracks()
    val topTracksShortTerm = database.topTrackDao.getShortTermTopTracks()

    /**
     * Actor to perform modifications to the user's library sequentially.
     *
     * Coroutine actors are marked as obsolete because they will be replaced in the future,
     * but they are still currently the best way to ensure async actions are performed sequentially.
     */
    @OptIn(ObsoleteCoroutinesApi::class)
    private val libraryActor = coroutineScope.actor<LibraryAction>(Dispatchers.IO, capacity = Channel.UNLIMITED) {
        for (action in channel) {
            when (action) {
                is LibraryAction.SaveAlbumToDb -> saveAlbumToDb(action.album)
                is LibraryAction.RemoveAlbumFromDb -> removeAlbumFromDb(action.albumId)
            }
        }
    }

    suspend fun saveAlbum(album: Album): Boolean {
        val success = spotifyApi.apiCall("add album ${album.id}") {
            library.add(LibraryType.Album, album.id)
        } != null

        if (success) {
            libraryActor.trySend(LibraryAction.SaveAlbumToDb(album))
        }
        return success
    }

    suspend fun removeAlbum(album: Album): Boolean {
        val success = spotifyApi.apiCall("remove album ${album.id}") {
            library.remove(LibraryType.Album, album.id)
        } != null

        if (success) {
            libraryActor.trySend(LibraryAction.RemoveAlbumFromDb(album.id))
        }
        return success
    }

    private suspend fun saveAlbumToDb(album: Album) {
        val genreData = populateGenreDataFromAlbums(listOf(album))
        val dbAlbum = album.toDbAlbum()
        updateDatabase("save album ${album.id}") {
            albumDao.insert(dbAlbum)
            genreDao.insertAll(genreData.genres)
            albumGenreJunctionDao.insertAll(genreData.albumGenreJunctions)
        }
    }

    private suspend fun removeAlbumFromDb(albumId: String) {
        updateDatabase("remove album $albumId") {
            genreDao.deleteForDeletedAlbum(albumId)
            albumGenreJunctionDao.deleteAllForAlbum(albumId)
            albumDao.delete(albumId)
        }
    }

    fun clearLibraryData() {
        coroutineScope.launch(Dispatchers.IO) {
            database.clearAllTables()
        }
    }

    /**
     * Run a Spotify library sync by requesting the user's latest library data from the Spotify API,
     * converting that data to Libzy's schema, and caching it in the local database.
     */
    suspend fun syncLibraryData(): Unit = withContext(Dispatchers.IO) {
        val recentlyPlayedTracks = async { spotifyApi.fetchPlayHistory().map { it.track } }
        val topTracksShortTerm = async { spotifyApi.fetchTopTracks(ShortTerm) }
        val topTracksMediumTerm = async { spotifyApi.fetchTopTracks(MediumTerm) }
        val topTracksLongTerm = async { spotifyApi.fetchTopTracks(LongTerm) }
        val albums = async { spotifyApi.fetchAllSavedAlbums().map { savedAlbum -> savedAlbum.album } }

        val dbAlbums = albums.await().map { album ->
            async {
                album.toDbAlbum(
                    recentlyPlayedTracks.await().map { it.id },
                    topTracksShortTerm.await().map { it.id },
                    topTracksMediumTerm.await().map { it.id },
                    topTracksLongTerm.await().map { it.id }
                )
            }
        }.awaitAll()

        val genreData = if (dbAlbums.isNotEmpty()) populateGenreDataFromAlbums(albums.await()) else DbGenreData()
        val dbTopTracks = topTracksShortTerm.await().map { it.toDbTopTrack(ShortTerm) }
            .plus(topTracksMediumTerm.await().map { it.toDbTopTrack(MediumTerm) })
            .plus(topTracksLongTerm.await().map { it.toDbTopTrack(LongTerm) })

        Timber.v("Saving library data in local database")
        updateDatabase("update cached library data") {
            albumDao.replaceAll(dbAlbums)
            genreDao.replaceAll(genreData.genres)
            albumGenreJunctionDao.replaceAll(genreData.albumGenreJunctions)
            topTrackDao.replaceAll(dbTopTracks)
        }.getOrThrow()
    }

    private suspend fun getAlbumAudioFeatures(album: Album): AudioFeaturesTuple {
        Timber.v("Fetching audio features for ${album.name}")
        // TODO: at some point we should invalidate cached audioFeatures in case Spotify's audio analysis has updated
        val cachedAudioFeatures = withContext(Dispatchers.IO) { database.albumDao.getAudioFeatures(album.id) }
        if (cachedAudioFeatures != null) return cachedAudioFeatures

        val albumTrackIds = album.tracks.items.map { it.id }
        val audioFeaturesOfTracks = spotifyApi.fetchAudioFeaturesOfTracks(albumTrackIds).filterNotNull()
        if (audioFeaturesOfTracks.isEmpty()) return AudioFeaturesTuple(0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F)

        fun findAudioFeatureAverage(getSpecificAudioFeature: (AudioFeatures) -> Float): Float =
            audioFeaturesOfTracks.map(getSpecificAudioFeature).average().toFloat().takeUnless { it.isNaN() } ?: 0.5f

        return AudioFeaturesTuple(
            findAudioFeatureAverage { it.valence },
            findAudioFeatureAverage { it.acousticness },
            findAudioFeatureAverage { it.instrumentalness },
            findAudioFeatureAverage { it.energy },
            findAudioFeatureAverage { it.danceability },
            findAudioFeatureAverage { it.liveness }
        )
    }

    private suspend fun populateGenreDataFromAlbums(albums: List<Album>): DbGenreData {
        Timber.v("Scanning genres in saved albums")
        val dbGenres = mutableSetOf<DbGenre>()
        val albumGenreJunctions = mutableSetOf<AlbumGenreJunction>()
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
        return DbGenreData(dbGenres, albumGenreJunctions)
    }

    private suspend fun Album.toDbAlbum(
        recentlyPlayedTrackIds: List<String>? = null,
        topTrackIdsShortTerm: List<String>? = null,
        topTrackIdsMediumTerm: List<String>? = null,
        topTrackIdsLongTerm: List<String>? = null
    ): DbAlbum {

        fun albumTracksAreInList(trackIds: List<String>) =
            tracks.items.any { trackIds.contains(it.id) }

        return DbAlbum(
            spotifyId = id,
            spotifyUri = uri.uri,
            title = name,
            artists = artists.joinToString { it.name },
            artworkUrl = artworkUrl,
            yearReleased = releaseDate.year,
            popularity = percentageToFloat(popularity),
            audioFeatures = getAlbumAudioFeatures(this),
            familiarity = FamiliarityTuple(
                albumTracksAreInList(recentlyPlayedTrackIds ?: spotifyApi.fetchPlayHistory().map { it.track.id }),
                albumTracksAreInList(topTrackIdsShortTerm ?: topTracksShortTerm.firstOrNull().orEmpty().map { it.spotifyId }),
                albumTracksAreInList(topTrackIdsMediumTerm ?: topTracksMediumTerm.firstOrNull().orEmpty().map { it.spotifyId }),
                albumTracksAreInList(topTrackIdsLongTerm ?: topTracksLongTerm.firstOrNull().orEmpty().map { it.spotifyId })
            )
        )
    }

    private fun Track.toDbTopTrack(timeRange: TimeRange) = DbTopTrack(
        spotifyId = id,
        spotifyUri = uri.uri,
        title = name,
        artists = artists.joinToString { it.name },
        albumId = album.id,
        timeRange = timeRange
    )

    suspend fun getAlbumFromUri(spotifyUri: String) = database.albumDao.getAlbumFromUri(spotifyUri)

    suspend fun getNumAlbumsSaved(): Int? = spotifyApi.apiCall("get number of albums saved") {
        library.getSavedAlbums().total
    }

    val enoughAlbumsSavedFlow = albums
        .map { it.size >= MINIMUM_NUM_ALBUMS_SAVED }
        .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = true)

    fun areEnoughAlbumsSaved() = enoughAlbumsSavedFlow.value

    private suspend fun <T> updateDatabase(
        updateDescription: String,
        update: suspend UserLibraryDatabase.() -> T
    ): Result<T> = runCatching {
        with(database) {
            withTransaction {
                update()
            }
        }
    }.onFailure {
        Timber.e("Database update failed - $updateDescription")
    }

    companion object {
        const val MINIMUM_NUM_ALBUMS_SAVED = 20
    }
}

private data class DbGenreData(
    val genres: Set<DbGenre> = emptySet(),
    val albumGenreJunctions: Set<AlbumGenreJunction> = emptySet()
)

private sealed interface LibraryAction {
    data class SaveAlbumToDb(val album: Album) : LibraryAction
    data class RemoveAlbumFromDb(val albumId: String) : LibraryAction
}