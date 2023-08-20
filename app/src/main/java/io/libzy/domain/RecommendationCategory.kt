package io.libzy.domain

import androidx.annotation.StringRes
import io.libzy.persistence.database.tuple.LibraryAlbum

/**
 * A group of recommended [LibraryAlbum]s with shared characteristic(s) indicated by its [relevance].
 */
data class RecommendationCategory(
    val relevance: Relevance,
    val albums: List<LibraryAlbum>
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
            @StringRes val adjectives: Collection<Int> = emptyList(), // TODO: use enum in domain layer and StringRes/TextResource in view layer
            val genre: String? = null,
            val familiarity: Query.Familiarity? = null,
        ) : Relevance {
            val numRelevantParameters = adjectives.size + listOfNotNull(genre, familiarity).size
        }
    }
}