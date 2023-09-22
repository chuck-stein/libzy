package io.libzy.recommendation

import com.adamratzman.spotify.endpoints.pub.ArtistApi.AlbumInclusionStrategy
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.RecommendationResponse
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.utils.Market
import io.libzy.analytics.AnalyticsConstants.EventProperties.CHOSEN_TECHNIQUE
import io.libzy.analytics.AnalyticsConstants.EventProperties.ERROR
import io.libzy.analytics.AnalyticsConstants.EventProperties.EXHAUSTED_TECHNIQUES
import io.libzy.analytics.AnalyticsConstants.EventProperties.LOAD_TIME_MILLIS
import io.libzy.analytics.AnalyticsConstants.EventProperties.NUM_RECOMMENDATIONS
import io.libzy.analytics.AnalyticsConstants.EventProperties.RECOMMENDED_ALBUMS
import io.libzy.analytics.AnalyticsConstants.EventProperties.TOTAL_RECOMMENDATIONS_SO_FAR
import io.libzy.analytics.AnalyticsConstants.Events.LOAD_LIBRARY_RECOMMENDATIONS
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.describe
import io.libzy.domain.duration
import io.libzy.recommendation.RecommendationTechnique.FromLibraryArtists
import io.libzy.recommendation.RecommendationTechnique.FromRecommendedArtists
import io.libzy.recommendation.RecommendationTechnique.FromRecommendedTracksForLibraryArtists
import io.libzy.recommendation.RecommendationTechnique.FromRecommendedTracksForRandomGenres
import io.libzy.recommendation.RecommendationTechnique.FromRecommendedTracksForTopTracks
import io.libzy.recommendation.RecommendationTechnique.FromTopTracks
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.api.SpotifyApiDelegator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.Closeable
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * Recommends albums to add to a user's library, based on their current library data.
 * Must be [close]d to free up resources once [recommendAlbums] will no longer being called.
 */
class LibraryRecommendationService @Inject constructor(
    private val spotifyApi: SpotifyApiDelegator,
    private val analytics: AnalyticsDispatcher,
    userLibraryRepository: UserLibraryRepository,
) : Closeable {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun close() {
        coroutineScope.cancel()
    }

    private val recommendationState = MutableStateFlow(LibraryRecommendationState())

    private val market = Market.FROM_TOKEN

    private val albumIdsInLibrary = userLibraryRepository.albums
        .map { albums -> albums.map { it.spotifyId } }
        .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = emptyList())

    private val seedAlbums = userLibraryRepository.albums
        .map { albums ->
            albums
                .asSequence()
                .shuffled()
                .sortedByDescending { it.familiarity }
                .map { it.spotifyId }
                .distinct()
                .filter { it !in recommendationState.value.albumIdsAlreadySeeded }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = emptySequence())

    private val seedTracks = combine(
        userLibraryRepository.topTracksLongTerm,
        userLibraryRepository.topTracksMediumTerm,
        userLibraryRepository.topTracksShortTerm
    ) { topTracksLongTerm, topTracksMediumTerm, topTracksShortTerm ->
        topTracksLongTerm.asSequence().shuffled()
            .plus(topTracksMediumTerm.asSequence().shuffled())
            .plus(topTracksShortTerm.asSequence().shuffled())
            .distinct()
    }

    private val seedTracksForAlbums = seedTracks
        .map { seedTracks ->
            seedTracks.filter { track ->
                track.spotifyId !in recommendationState.value.trackIdsAlreadySeededForAlbums
                        && track.albumId.canBeARecommendedAlbumId()
            }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = emptySequence())

    private val seedTracksForOtherTracks = seedTracks
        .map { seedTracks ->
            seedTracks
                .map { it.spotifyId }
                .filter { it !in recommendationState.value.trackIdsAlreadySeededForTracks }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = emptySequence())

    private val seedArtistsForOtherArtists = recommendationState
        .map { it.unusedArtistIdsForSeedingOtherArtists.asSequence().distinct() }
        .distinctUntilChanged()
        .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = emptySequence())

    private val seedArtistsForTracks = recommendationState
        .map { it.unusedArtistIdsForSeedingTracks.asSequence().distinct() }
        .distinctUntilChanged()
        .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = emptySequence())

    /**
     * Paginated album recommender.
     *
     * Outputs a group of albums we think might be a good fit for the user to save to their Spotify library. Each page
     * size should be roughly in the low double digits, and will not include albums already recommended in a previous
     * page or already saved in the user's library.
     *
     * Each page will follow a certain recommendation strategy. We will either:
     * - Recommend albums containing the user's top tracks
     * - Recommend albums containing tracks that Spotify recommends based on the user's top tracks
     * - Recommend albums containing tracks that Spotify recommends based on artists in the user's library
     * - Recommend albums by the same artists as the user's saved albums
     * - Recommend albums by artists related to the artist of the user's saved albums
     * - Recommend albums from random genres (only if user has no saved albums or top tracks)
     *
     * @return A list of recommended albums, or null if we failed to find recommendations (for example due to API call failures)
     */
    suspend fun recommendAlbums(): List<Album>? = withContext(Dispatchers.Default) {
        if (recommendationState.value.noRecommendationsYetThisSession) {
            awaitRecommendationDependencies()
        }
        val exhaustedTechniques = determineExhaustedTechniques()
        val chosenTechnique = chooseRecommendationTechnique(exhaustedTechniques)
        val recommendedAlbumsTimedValue = measureTimedValue {
            when (chosenTechnique) {
                FromTopTracks -> recommendAlbumsFromTopTracks()
                FromLibraryArtists -> recommendAlbumsFromLibraryArtists()
                FromRecommendedArtists -> recommendAlbumsFromRecommendedArtists()
                FromRecommendedTracksForTopTracks -> recommendAlbumsFromRecommendedTracksForTopTracks()
                FromRecommendedTracksForLibraryArtists -> recommendAlbumsFromRecommendedTracksForLibraryArtists()
                FromRecommendedTracksForRandomGenres -> recommendAlbumsFromRandomGenres()
            }
        }
        val (recommendedAlbums, timeToRecommend) = recommendedAlbumsTimedValue

        if (recommendedAlbums != null) {
            addRecommendedAlbums(recommendedAlbums)
        }
        analytics.sendEvent(
            eventName = LOAD_LIBRARY_RECOMMENDATIONS,
            eventProperties = mapOf(
                ERROR to (recommendedAlbums == null),
                NUM_RECOMMENDATIONS to recommendedAlbums.orEmpty().size,
                CHOSEN_TECHNIQUE to chosenTechnique.techniqueName,
                EXHAUSTED_TECHNIQUES to exhaustedTechniques.map { it.techniqueName },
                LOAD_TIME_MILLIS to timeToRecommend.inWholeMilliseconds,
                RECOMMENDED_ALBUMS to recommendedAlbums.orEmpty().joinToString { it.describe() },
                TOTAL_RECOMMENDATIONS_SO_FAR to recommendationState.value.albumIdsAlreadyRecommendedThisSession.size
            )
        )
        recommendedAlbums
    }

    private fun determineExhaustedTechniques() = setOfNotNull(
        FromTopTracks.takeIf { seedTracksForAlbums.value.none() },
        FromLibraryArtists.takeIf { seedAlbums.value.none() },
        FromRecommendedArtists.takeIf { seedArtistsForOtherArtists.value.none() },
        FromRecommendedTracksForTopTracks.takeIf { seedTracksForOtherTracks.value.none() },
        FromRecommendedTracksForLibraryArtists.takeIf { seedArtistsForTracks.value.none() },
    )

    private fun chooseRecommendationTechnique(
        exhaustedTechniques: Set<RecommendationTechnique>
    ): RecommendationTechnique = with(recommendationState.value) {

        val techniquePool = setOf(
            FromRecommendedTracksForTopTracks,
            FromRecommendedTracksForLibraryArtists,
            FromTopTracks,
            FromLibraryArtists,
            FromRecommendedArtists
        )

        val weightedTechniquePool = techniquePool
            .minus(exhaustedTechniques)
            .filter { it != previousTechnique || techniquePool.size == 1 }
            .map { technique ->
                List(technique.priorityWeight) { technique }
            }.flatten()

        val chosenTechnique =
            if (noRecommendationsYetThisSession && FromRecommendedTracksForTopTracks in weightedTechniquePool) {
                FromRecommendedTracksForTopTracks // prefer this technique for the first albums "above the fold"
            } else {
                weightedTechniquePool.randomOrNull() ?: FromRecommendedTracksForRandomGenres
            }

        recommendationState.update {
            it.copy(previousTechnique = chosenTechnique)
        }
        return chosenTechnique
    }

    private suspend fun recommendAlbumsFromTopTracks(): List<Album>? {
        val chosenSeedTracks = seedTracksForAlbums.value
            .take(MAX_TRACK_SEEDS_FOR_ALBUMS)
            .toList()

        return chosenSeedTracks
            .map { it.albumId }
            .fetchAlbumDetailsFromIds()
            ?.toSet()
            ?.filterOutSingles().also { recommendations ->
                if (recommendations != null) {
                    recommendationState.update { state ->
                        val chosenSeedTrackIds = chosenSeedTracks.map { it.spotifyId }
                        state.copy(trackIdsAlreadySeededForAlbums = state.trackIdsAlreadySeededForAlbums.plus(chosenSeedTrackIds))
                    }
                }
            }
    }

    private suspend fun recommendAlbumsFromLibraryArtists(): List<Album>? {
        val chosenSeedAlbums = seedAlbums.value.take(MAX_ARTIST_SEEDS_FOR_ALBUMS).toList()
        return chosenSeedAlbums
            .fetchAlbumDetailsFromIds()
            ?.map { it.artists }
            ?.flatten()
            ?.map { it.id }
            ?.minus(recommendationState.value.artistIdsAlreadySeededForAlbums)
            ?.updateCachedArtistIds()
            ?.fetchAlbumsFromArtistIds()
            ?.map { it.id }
            ?.shuffled()
            ?.fetchAlbumDetailsFromIds().also { recommendations ->
                if (recommendations != null) {
                    recommendationState.update { state ->
                        state.copy(albumIdsAlreadySeeded = state.albumIdsAlreadySeeded.plus(chosenSeedAlbums))
                    }
                }
            }
    }

    private suspend fun recommendAlbumsFromRecommendedArtists(): List<Album>? {
        val chosenSeedArtists = seedArtistsForOtherArtists.value.take(MAX_ARTIST_SEEDS_FOR_OTHER_ARTISTS).toList()
        return chosenSeedArtists
            .fetchRelatedArtistsFromArtistIds()
            ?.map { it.id }
            ?.minus(recommendationState.value.cachedArtistIdsInLibrary.toSet())
            ?.take(MAX_ARTIST_SEEDS_FOR_ALBUMS)
            ?.fetchAlbumsFromArtistIds()
            ?.map { it.id }
            ?.shuffled()
            ?.fetchAlbumDetailsFromIds().also { recommendations ->
                if (recommendations != null) {
                    recommendationState.update { state ->
                        state.copy(
                            artistIdsAlreadySeededForOtherArtists = state.artistIdsAlreadySeededForOtherArtists.plus(chosenSeedArtists)
                        )
                    }
                }
            }
    }

    private suspend fun recommendAlbumsFromRecommendedTracksForTopTracks(): List<Album>? {
        val chosenSeedTracks = seedTracksForOtherTracks.value.take(MAX_SEEDS_FOR_SPOTIFY_RECOMMENDATIONS).toList()
        val recommendedTracks = spotifyApi.apiCall("get recommended tracks based on seeded top tracks") {
            browse.getRecommendations(
                seedTracks = chosenSeedTracks,
                limit = MAX_SPOTIFY_RECOMMENDATIONS_TO_FETCH,
                market = market
            )
        }
        return recommendAlbumsFrom(recommendedTracks).also { recommendations ->
            if (recommendations != null) {
                recommendationState.update { state ->
                    state.copy(trackIdsAlreadySeededForTracks = state.trackIdsAlreadySeededForTracks.plus(chosenSeedTracks))
                }
            }
        }
    }

    private suspend fun recommendAlbumsFromRecommendedTracksForLibraryArtists(): List<Album>? {
        val chosenSeedArtists = seedArtistsForTracks.value.take(MAX_SEEDS_FOR_SPOTIFY_RECOMMENDATIONS).toList()
        val recommendedTracks = spotifyApi.apiCall("get recommended tracks based on seeded artists") {
            browse.getRecommendations(
                seedArtists = chosenSeedArtists,
                limit = MAX_SPOTIFY_RECOMMENDATIONS_TO_FETCH,
                market = market
            )
        }
        return recommendAlbumsFrom(recommendedTracks).also { recommendations ->
            if (recommendations != null) {
                recommendationState.update { state ->
                    state.copy(artistIdsAlreadySeededForTracks = state.artistIdsAlreadySeededForTracks.plus(chosenSeedArtists))
                }
            }
        }
    }

    private suspend fun recommendAlbumsFromRandomGenres(): List<Album>? {
        val genreSeeds = spotifyApi.apiCall("get available genre seeds") {
            browse.getAvailableGenreSeeds()
        } ?: return null

        val recommendedTracks = spotifyApi.apiCall("get recommended tracks based on random genres") {
            browse.getRecommendations(
                seedGenres = genreSeeds.shuffled().take(MAX_SEEDS_FOR_SPOTIFY_RECOMMENDATIONS),
                limit = MAX_SPOTIFY_RECOMMENDATIONS_TO_FETCH,
                market = market
            )
        }
        return recommendAlbumsFrom(recommendedTracks)
    }

    private suspend fun recommendAlbumsFrom(recommendedTracks: RecommendationResponse?) =
        recommendedTracks?.tracks
            ?.map { it.album.id }
            ?.filter { it.canBeARecommendedAlbumId() }
            ?.fetchAlbumDetailsFromIds()
            ?.toSet()
            ?.filterOutSingles()

    private suspend fun List<String>.fetchRelatedArtistsFromArtistIds() = coroutineScope {
        map { artistId ->
            async {
                spotifyApi.apiCall("fetch artists related to artist with ID $artistId") {
                    artists.getRelatedArtists(artistId)
                }
            }
        }.awaitAll()
            .filterToSuccessesOrNull()
            ?.flatten()
            ?.shuffled()
            ?.toSet()
    }

    private suspend fun Iterable<String>.fetchAlbumDetailsFromIds(): List<Album>? = coroutineScope {
        toSet()
            .map { albumId ->
                async {
                    spotifyApi.apiCall("get album $albumId for library recommendations") {
                        albums.getAlbum(albumId, market = market)
                    }
                }
            }.awaitAll()
            .filterToSuccessesOrNull()
    }

    private suspend fun Iterable<String>.fetchAlbumsFromArtistIds(): List<SimpleAlbum>? = coroutineScope {
        toSet()
            .map { artistId ->
                async {
                    spotifyApi.apiCall("get artist $artistId for library recommendations") {
                        artists.getArtistAlbums(
                            artistId,
                            limit = SpotifyApiDelegator.API_ITEM_LIMIT_LOW,
                            offset = 0,
                            market,
                            AlbumInclusionStrategy.Album
                        )
                    }.also {
                        if (it != null) {
                            recommendationState.update { state ->
                                state.copy(artistIdsAlreadySeededForAlbums = state.artistIdsAlreadySeededForAlbums + artistId)
                            }
                        }
                    }
                }
            }.awaitAll()
            .filterToSuccessesOrNull()
            ?.map { albumsByArtist ->
                albumsByArtist.items
                    .filter { it.id.canBeARecommendedAlbumId() }
                    .shuffled()
                    .take(MAX_ALBUMS_TO_RECOMMEND_FROM_SINGLE_ARTIST)
            }
            ?.flatten()
    }

    private fun addRecommendedAlbums(albums: List<Album>) {
        val recommendedAlbumIds = albums.map { it.id }
        recommendationState.update { state ->
            state.copy(
                albumIdsAlreadyRecommended = state.albumIdsAlreadyRecommended.plus(recommendedAlbumIds),
                albumIdsAlreadyRecommendedThisSession = state.albumIdsAlreadyRecommendedThisSession.plus(recommendedAlbumIds)
            )
        }
    }

    private fun List<String>.updateCachedArtistIds(): List<String> {
        recommendationState.update { state ->
            state.copy(cachedArtistIdsInLibrary = state.cachedArtistIdsInLibrary.plus(this))
        }
        return this
    }

    private suspend fun awaitRecommendationDependencies() {
        val checkInterval = 50.milliseconds
        val maxWaitTime = 3.seconds
        var waitTime = Duration.ZERO

        while (waitTime < maxWaitTime && !dependenciesAreReady()) {
            delay(checkInterval)
            waitTime += checkInterval
        }
        if (waitTime >= maxWaitTime) {
            Timber.e("failed to populate recommendation seeds in $maxWaitTime")
        }
    }

    private fun dependenciesAreReady() =
        albumIdsInLibrary.value.any() && seedAlbums.value.any() && seedTracksForOtherTracks.value.any()

    private fun String.canBeARecommendedAlbumId() =
        this !in albumIdsInLibrary.value && this !in recommendationState.value.albumIdsAlreadyRecommended

    private fun Iterable<Album>.filterOutSingles() = filter { album -> album.duration > 15.minutes }

    private fun <T> Iterable<T?>.filterToSuccessesOrNull(): List<T>? = filterNotNull().takeIf { any { it != null } }
}

private data class LibraryRecommendationState(
    val albumIdsAlreadyRecommended: Set<String> = emptySet(),
    val albumIdsAlreadyRecommendedThisSession: Set<String> = emptySet(),
    val trackIdsAlreadySeededForTracks: Set<String> = emptySet(),
    val trackIdsAlreadySeededForAlbums: Set<String> = emptySet(),
    val artistIdsAlreadySeededForOtherArtists: Set<String> = emptySet(),
    val artistIdsAlreadySeededForTracks: Set<String> = emptySet(),
    val artistIdsAlreadySeededForAlbums: Set<String> = emptySet(),
    val albumIdsAlreadySeeded: Set<String> = emptySet(),
    val cachedArtistIdsInLibrary: List<String> = emptyList(), // roughly ordered by descending familiarity
    val previousTechnique: RecommendationTechnique? = null
) {
    val noRecommendationsYetThisSession = previousTechnique == null
    val unusedArtistIdsForSeedingOtherArtists = cachedArtistIdsInLibrary - artistIdsAlreadySeededForOtherArtists
    val unusedArtistIdsForSeedingTracks = cachedArtistIdsInLibrary - artistIdsAlreadySeededForTracks
}

enum class RecommendationTechnique(val techniqueName: String, val priorityWeight: Int) {
    FromRecommendedTracksForTopTracks("FromRecommendedTracksForTopTracks", priorityWeight = 3),
    FromRecommendedTracksForLibraryArtists("FromRecommendedTracksForLibraryArtists", priorityWeight = 3),
    FromTopTracks("FromTopTracks", priorityWeight = 5),
    FromLibraryArtists("FromLibraryArtists", priorityWeight = 2),
    FromRecommendedArtists("FromRecommendedArtists", priorityWeight = 1),
    FromRecommendedTracksForRandomGenres("FromRecommendedTracksForRandomGenres", priorityWeight = 0);
}

private const val MAX_SEEDS_FOR_SPOTIFY_RECOMMENDATIONS = 5
private const val MAX_TRACK_SEEDS_FOR_ALBUMS = 15
private const val MAX_ARTIST_SEEDS_FOR_ALBUMS = 10
private const val MAX_ARTIST_SEEDS_FOR_OTHER_ARTISTS = 10
private const val MAX_ALBUMS_TO_RECOMMEND_FROM_SINGLE_ARTIST = 2
private const val MAX_SPOTIFY_RECOMMENDATIONS_TO_FETCH = 25