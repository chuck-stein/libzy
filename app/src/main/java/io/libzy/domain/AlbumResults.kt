package io.libzy.domain

import androidx.annotation.StringRes
import io.libzy.persistence.database.tuple.LibraryAlbum

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
