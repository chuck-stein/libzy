package com.chuckstein.libzy.recommendation

import com.chuckstein.libzy.database.tuple.FamiliarityTuple
import com.chuckstein.libzy.database.tuple.LibraryAlbum
import com.chuckstein.libzy.model.AlbumResult
import com.chuckstein.libzy.model.Query
import javax.inject.Inject
import kotlin.math.abs

// TODO: make this a singleton?
// TODO: handle more edge cases, including different size/variety/type of library
class RecommendationService @Inject constructor() {

    companion object {
        private const val RELEVANCE_THRESHOLD = 0.5 // TODO: decide on best value
    }

    // TODO: restructure recommendGenres/recommendAlbums so that they are LiveData properties of themselves, and update when query updates or libraryAlbums updates
    // TODO: check this algorithm
    // TODO: don't get libraryAlbums from param, but from userLibraryRepository directly
    // TODO: make this a computed value rather than function
    fun recommendGenres(libraryAlbums: List<LibraryAlbum>, query: Query): List<String> {
        val genresToRelevance = mutableMapOf<String, Float>()
        val albumsToRelevance = calculateRelevanceOfAlbums(libraryAlbums, query)
        val relevantAlbums = libraryAlbums.filter { albumIsRelevant(it, albumsToRelevance) }
        for (album in relevantAlbums) {
            albumsToRelevance[album].let { albumRelevance ->
                if (albumRelevance != null) {
                    for (genre in album.genres) {
                        genresToRelevance[genre].let { genreRelevance ->
                            if (genreRelevance == null) genresToRelevance[genre] = albumRelevance
                            else genresToRelevance[genre] = genreRelevance + albumRelevance
                        }
                    }
                }
            }
        }
        return genresToRelevance.keys.sortedByDescending { genresToRelevance[it] }
    }

    // TODO: improve output by presenting a mix of albums that best satisfy different parts of the query
    fun recommendAlbums(libraryAlbums: List<LibraryAlbum>, query: Query): List<AlbumResult> {
        // TODO: ensure DbAlbum is hashable as map key because it's a data class
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

        fun <T, V> calculateAlbumRelevanceCategory(queryParam: T?, calculateRelevance: (T) -> V): V? {
            // a null relevance score means the user has no preference for this relevance category
            return if (queryParam == null) null else calculateRelevance(queryParam)
        }

        val genreRelevance: Boolean? = calculateAlbumRelevanceCategory(query.genres) { genres ->
            calculateGenreRelevance(album.genres, genres)
        }

        if (genreRelevance != null && !genreRelevance) return 0F // only show results that are of a selected genre

        val familiarityRelevance: Boolean? = calculateAlbumRelevanceCategory(query.familiarity) { familiarity ->
            calculateFamiliarityRelevance(album.familiarity, familiarity)
        }

        if (familiarityRelevance != null && !familiarityRelevance) return 0F // only show results of selected familiarity

        val instrumentalnessRelevance: Float? = calculateAlbumRelevanceCategory(query.instrumental) { instrumental ->
            calculateInstrumentalnessRelevance(album.audioFeatures.instrumentalness, instrumental)
        }
        val acousticnessRelevance: Float? = calculateAlbumRelevanceCategory(query.acousticness) { acousticness ->
            calculateGenericQueryCategoryRelevance(album.audioFeatures.acousticness, acousticness)
        }
        val valenceRelevance: Float? = calculateAlbumRelevanceCategory(query.valence) { valence ->
            calculateGenericQueryCategoryRelevance(album.audioFeatures.valence, valence)
        }
        val energyRelevance: Float? = calculateAlbumRelevanceCategory(query.energy) { energy ->
            calculateGenericQueryCategoryRelevance(album.audioFeatures.energy, energy)
        }
        val danceabilityRelevance: Float? = calculateAlbumRelevanceCategory(query.danceability) { danceability ->
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

    // TODO: make this more accurate by making related genres somewhat relevant, even if they weren't selected explicitly
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