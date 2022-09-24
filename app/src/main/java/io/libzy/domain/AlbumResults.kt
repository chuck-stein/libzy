package io.libzy.domain

import android.content.res.Resources
import androidx.annotation.StringRes
import io.libzy.R
import io.libzy.persistence.database.tuple.LibraryAlbum
import io.libzy.util.capitalizeAllWords
import io.libzy.util.joinToUserFriendlyString

/**
 * An album that is recommended to the user based on their listening mood [Query].
 */
data class AlbumResult(
    val title: String,
    val artists: String,
    val spotifyUri: String,
    val artworkUrl: String? = null
)

fun LibraryAlbum.toAlbumResult() = AlbumResult(title, artists, spotifyUri, artworkUrl)


/**
 * A group of recommended [AlbumResult]s with shared characteristic(s) indicated by its [relevance].
 */
data class RecommendationCategory(
    val relevance: Relevance,
    val albumResults: List<AlbumResult>
) {
    /**
     * A classification of how a [RecommendationCategory] is relevant to the user's mood-based [Query].
     */
    sealed interface Relevance {

        /**
         * Indicates that a [RecommendationCategory] is relevant to all parameters of the user's mood-based [Query].
         */
        object Full : Relevance

        /**
         * Indicates that a [RecommendationCategory] is relevant to some, but not all,
         * of the parameters of the user's mood-based [Query].
         *
         * @property adjectives Resource IDs corresponding with strings that describe which [Query] parameters are
         *                      relevant to this category (not including the genre and familiarity parameters).
         * @property genre A string indicating the genre that is associated with this category, or null if none.
         * @property familiarity The level of familiarity that is associated with this category, or null if none.
         */
        data class Partial(
            @StringRes val adjectives: Collection<Int> = emptyList(),
            val genre: String? = null,
            val familiarity: Query.Familiarity? = null,
        ) : Relevance {
            val numRelevantParameters = adjectives.size + listOfNotNull(genre, familiarity).size
        }
    }
}

fun RecommendationCategory.title(resources: Resources) = when (relevance) {

    is RecommendationCategory.Relevance.Full -> resources.getString(R.string.full_match_category_title)

    is RecommendationCategory.Relevance.Partial -> {

        val adjectiveString = relevance.adjectives.map { resources.getString(it) }.joinToUserFriendlyString()
        val capitalizedGenre = relevance.genre?.capitalizeAllWords()

        val nounString = when (relevance.familiarity) {
            Query.Familiarity.CURRENT_FAVORITE -> capitalizedGenre?.let {
                resources.getString(R.string.current_genre_favorites, it)
            } ?: resources.getString(R.string.current_favorites)

            Query.Familiarity.RELIABLE_CLASSIC -> capitalizedGenre?.let {
                resources.getString(R.string.reliable_genre_classics, it)
            } ?: resources.getString(R.string.reliable_classics)

            Query.Familiarity.UNDERAPPRECIATED_GEM -> capitalizedGenre?.let {
                resources.getString(R.string.underappreciated_genre, it)
            } ?: resources.getString(R.string.underappreciated_gems)

            null -> capitalizedGenre.orEmpty()
        }

        buildString {
            append(adjectiveString)
            if (adjectiveString.isNotEmpty() && nounString.isNotEmpty()) {
                append(" ")
            }
            append(nounString)
        }
    }
}