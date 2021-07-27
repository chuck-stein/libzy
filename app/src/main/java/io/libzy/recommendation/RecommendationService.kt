package io.libzy.recommendation

import io.libzy.domain.AlbumResult
import io.libzy.domain.Query
import io.libzy.persistence.database.tuple.FamiliarityTuple
import io.libzy.persistence.database.tuple.LibraryAlbum
import javax.inject.Inject
import kotlin.math.abs

// TODO: make this a Dagger @Singleton?
class RecommendationService @Inject constructor() {

    companion object {
        private const val RELEVANCE_THRESHOLD = 0.5
    }

    // TODO: clean up this logic and syntax
    fun recommendGenres(query: Query, libraryAlbums: List<LibraryAlbum>): List<String> {
        val genresToRelevance = mutableMapOf<String, Float>()

        // calculate album relevance to recommend genres based on all non-genre parameters of the given query
        val albumsToRelevance = calculateRelevanceOfAlbums(libraryAlbums, query.copy(genres = null))
        val relevantAlbums = libraryAlbums.filter { albumIsRelevant(it, albumsToRelevance) }
        for (album in relevantAlbums) {
            albumsToRelevance[album]?.let { albumRelevance ->
                for (genre in album.genres) {
                    genresToRelevance[genre].let { genreRelevance ->
                        if (genreRelevance == null) genresToRelevance[genre] = albumRelevance
                        else genresToRelevance[genre] = genreRelevance + albumRelevance
                    }
                }
            }
        }
        return genresToRelevance.keys.sortedByDescending { genresToRelevance[it] }
    }

    fun recommendAlbums(query: Query, libraryAlbums: List<LibraryAlbum>): List<AlbumResult> {
        val albumsToRelevance = calculateRelevanceOfAlbums(libraryAlbums, query)
        return libraryAlbums
            .sortedByDescending { albumsToRelevance[it] }
            .takeWhile { albumIsRelevant(it, albumsToRelevance) }
            .map {
                AlbumResult(
                    it.title,
                    it.artists,
                    it.artworkUrl,
                    it.spotifyUri
                )
            }
    }

    private fun calculateRelevanceOfAlbums(albums: List<LibraryAlbum>, query: Query): Map<LibraryAlbum, Float> =
        albums.map { it to calculateAlbumRelevance(it, query) }.toMap()

    private fun albumIsRelevant(album: LibraryAlbum, albumsToRelevance: Map<LibraryAlbum, Float>): Boolean =
        albumsToRelevance[album].let { it != null && it >= RELEVANCE_THRESHOLD }

    private fun calculateAlbumRelevance(album: LibraryAlbum, query: Query): Float {

        fun <T, V> calculateRelevanceIfNeeded(queryParam: T?, calculateRelevance: (T) -> V): V? {
            // a null relevance score means the user has no preference for this relevance category
            return queryParam?.let { calculateRelevance(queryParam) }
        }

        val genreRelevance: Boolean? = calculateRelevanceIfNeeded(query.genres?.takeIf { it.isNotEmpty() }) { genres ->
            calculateGenreRelevance(album.genres, genres)
        }

        if (genreRelevance != null && !genreRelevance) return 0F // only show results that are of a selected genre

        val familiarityRelevance: Boolean? = calculateRelevanceIfNeeded(query.familiarity) { familiarity ->
            calculateFamiliarityRelevance(album.familiarity, familiarity)
        }

        if (familiarityRelevance != null && !familiarityRelevance) return 0F // only show results of selected familiarity

        val instrumentalnessRelevance: Float? = calculateRelevanceIfNeeded(query.instrumental) { instrumental ->
            calculateInstrumentalnessRelevance(album.audioFeatures.instrumentalness, instrumental)
        }
        val acousticnessRelevance: Float? = calculateRelevanceIfNeeded(query.acousticness) { acousticness ->
            calculateGenericQueryCategoryRelevance(album.audioFeatures.acousticness, acousticness)
        }
        val valenceRelevance: Float? = calculateRelevanceIfNeeded(query.valence) { valence ->
            calculateGenericQueryCategoryRelevance(album.audioFeatures.valence, valence)
        }
        val energyRelevance: Float? = calculateRelevanceIfNeeded(query.energy) { energy ->
            calculateGenericQueryCategoryRelevance(album.audioFeatures.energy, energy)
        }
        val danceabilityRelevance: Float? = calculateRelevanceIfNeeded(query.danceability) { danceability ->
            calculateGenericQueryCategoryRelevance(album.audioFeatures.danceability, danceability)
        }

        val relevanceScoresToBeAveraged = listOfNotNull(
            instrumentalnessRelevance,
            acousticnessRelevance,
            valenceRelevance,
            energyRelevance,
            danceabilityRelevance
        )

        // if the user has no preference for any of the remaining relevance categories, then the album is relevant
        if (relevanceScoresToBeAveraged.isEmpty()) return 1F

        return relevanceScoresToBeAveraged.sum() / relevanceScoresToBeAveraged.size
    }

    private fun calculateGenreRelevance(genres: Set<String>, preferredGenres: Set<String>) =
        genres.any { it in preferredGenres }

    private fun calculateFamiliarityRelevance(familiarity: FamiliarityTuple, preferredFamiliarity: Query.Familiarity) =
        when (preferredFamiliarity) {
            Query.Familiarity.CURRENT_FAVORITE -> familiarity.recentlyPlayed || familiarity.shortTermFavorite
            Query.Familiarity.RELIABLE_CLASSIC -> familiarity.mediumTermFavorite || familiarity.longTermFavorite
            Query.Familiarity.UNDERAPPRECIATED_GEM -> familiarity.isLowFamiliarity()
        }

    private fun calculateInstrumentalnessRelevance(instrumentalness: Float, prefersInstrumental: Boolean) =
        if (prefersInstrumental) instrumentalness else 1 - instrumentalness

    private fun calculateGenericQueryCategoryRelevance(categoryValue: Float, categoryPreference: Float) =
        1 - abs(categoryPreference - categoryValue)

}
