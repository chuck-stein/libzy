package com.chuckstein.libzy.model

/**
 * Represents a user's mood / listening preferences, as a collection of desired album properties.
 * 
 * Null values indicate no preference for the associated property.
 */
data class Query(
    val familiarity: Familiarity? = null,
    val instrumental: Boolean? = null,
    val acousticness: Float? = null,
    val valence: Float? = null,
    val energy: Float? = null,
    val danceability: Float? = null,
    val genres: Set<String>? = null
) {
    enum class Familiarity {
        CURRENT_FAVORITE, RELIABLE_CLASSIC, UNDERAPPRECIATED_GEM
    }
}
