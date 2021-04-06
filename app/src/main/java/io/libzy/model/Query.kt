package io.libzy.model

import androidx.annotation.Keep

/**
 * Represents a user's mood / listening preferences, as a collection of desired album properties.
 * 
 * Null values indicate no preference for the associated property.
 */
@Keep
data class Query(
    val familiarity: Familiarity? = null,
    val instrumental: Boolean? = null,
    val acousticness: Float? = null,
    val valence: Float? = null,
    val energy: Float? = null,
    val danceability: Float? = null,
    val genres: Set<String>? = null
) {
    @Keep enum class Familiarity(val value: String) {
        CURRENT_FAVORITE("current favorite"),
        RELIABLE_CLASSIC("reliable classic"),
        UNDERAPPRECIATED_GEM("underappreciated gem")
    }
}
