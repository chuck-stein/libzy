package com.chuckstein.libzy.recommendation

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.chuckstein.libzy.database.entity.DbAlbum
import com.chuckstein.libzy.database.tuple.LibraryAlbum
import com.chuckstein.libzy.database.tuple.FamiliarityTuple
import com.chuckstein.libzy.model.AlbumResult
import com.chuckstein.libzy.model.Query
import javax.inject.Inject
import kotlin.math.abs

// TODO: handle more edge cases, including different size/variety/type of library
class RecommendationService @Inject constructor() {

    companion object {
        private const val MAX_RECOMMENDATIONS = 100 // TODO: decide on best value
        private const val RELEVANCE_THRESHOLD = 0.5 // TODO: decide on best value
    }

    // TODO: restructure recommendGenres/recommendAlbums so that they are LiveData properties of themselves, and update when query updates or libraryAlbums updates
    fun recommendGenres(libraryAlbums: LiveData<List<LibraryAlbum>>, query: Query): LiveData<List<String>> =
        Transformations.map(libraryAlbums) { albums ->
            val genresToRelevance = mutableMapOf<String, Double>()
            val albumsToRelevance = calculateRelevanceOfAlbums(albums, query)
            val relevantAlbums = albums.filter { albumIsRelevant(it, albumsToRelevance) }
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
            return@map genresToRelevance.keys.sortedByDescending { genresToRelevance[it] }
        }

    // TODO: improve output by presenting a mix of albums that best satisfy different parts of the query
    fun recommendAlbums(libraryAlbums: LiveData<List<LibraryAlbum>>, query: Query): LiveData<List<AlbumResult>> =
        Transformations.map(libraryAlbums) { albums ->

            // TODO: ensure DbAlbum is hashable as map key because it's a data class
            val albumsToRelevance = calculateRelevanceOfAlbums(albums, query)
            return@map albums
                .sortedByDescending { albumsToRelevance[it] }
                .take(MAX_RECOMMENDATIONS)
                .filter { albumIsRelevant(it, albumsToRelevance) }
                .map {
                    AlbumResult(
                        it.title,
                        it.artists,
                        it.artworkUrl,
                        it.spotifyUri
                    )
                }
        }
    
    private fun calculateRelevanceOfAlbums(albums: List<LibraryAlbum>, query: Query): Map<LibraryAlbum, Double> =
        albums.map { it to calculateAlbumRelevance(it, query) }.toMap()
    
    private fun albumIsRelevant(album: LibraryAlbum, albumsToRelevance: Map<LibraryAlbum, Double>): Boolean =
        albumsToRelevance[album].let { it != null && it >= RELEVANCE_THRESHOLD }

    private fun calculateAlbumRelevance(album: LibraryAlbum, query: Query): Double {
        var numRelevanceCategories = 0
        var categoryRelevanceSum = 0.0

        fun <T> processCategory(queryParam: T?, calculateRelevance: (T) -> Float) {
            if (queryParam != null) {
                numRelevanceCategories++
                categoryRelevanceSum += calculateRelevance(queryParam)
            }
        }

        processCategory(query.familiarity) { familiarity ->
            calculateFamiliarityRelevance(album.familiarity, familiarity)
        }
        processCategory(query.instrumental) { instrumental ->
            calculateInstrumentalnessRelevance(album.audioFeatures.instrumentalness, instrumental)
        }
        processCategory(query.acousticness) { acousticness ->
            calculateCategoryRelevance(album.audioFeatures.acousticness, acousticness)
        }
        processCategory(query.valence) { valence ->
            calculateCategoryRelevance(album.audioFeatures.valence, valence)
        }
        processCategory(query.energy) { energy ->
            calculateCategoryRelevance(album.audioFeatures.energy, energy)
        }
        processCategory(query.danceability) { danceability ->
            calculateCategoryRelevance(album.audioFeatures.danceability, danceability)
        }
        processCategory(query.genres) { genres ->
            calculateGenreRelevance(album.genres, genres)
        }

        return if (numRelevanceCategories == 0) 0.5 else categoryRelevanceSum / numRelevanceCategories
    }

    // TODO: maybe change this algorithm to take a harder hit on overall relevance if not relevant to familiarity preference
    // TODO: clean up this function
    private fun calculateFamiliarityRelevance(familiarity: FamiliarityTuple, preferredFamiliarity: Query.Familiarity) =
        when (preferredFamiliarity) {
            Query.Familiarity.CURRENT_FAVORITE -> if (familiarity.recentlyPlayed || familiarity.shortTermFavorite) 1F else if (familiarity.mediumTermFavorite) 0.3F else if (familiarity.longTermFavorite) 0.2F else 0F // TODO: refine this algorithm, remove magic numbers
            Query.Familiarity.RELIABLE_CLASSIC -> if (familiarity.mediumTermFavorite || familiarity.longTermFavorite) 1F else if (familiarity.shortTermFavorite) 0.3F else 0F
            Query.Familiarity.UNDERAPPRECIATED_GEM -> if (familiarity.recentlyPlayed || familiarity.shortTermFavorite || familiarity.mediumTermFavorite || familiarity.longTermFavorite) 0F else 1F
        }

    private fun calculateInstrumentalnessRelevance(instrumentalness: Float, prefersInstrumental: Boolean) =
        if (prefersInstrumental) instrumentalness else 1 - instrumentalness

    private fun calculateCategoryRelevance(categoryValue: Float, categoryPreference: Float) =
        1 - abs(categoryPreference - categoryValue)

    // TODO: make this more accurate by making related genres somewhat relevant, even if they weren't selected explicitly
    private fun calculateGenreRelevance(genres: Set<String>, preferredGenres: Set<String>) =
        if (genres.any { it in preferredGenres }) 1F else 0F

}