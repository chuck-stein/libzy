package io.libzy.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a user's mood / listening preferences, as a collection of desired album properties.
 * 
 * Null values indicate no preference for the associated property.
 */
@Parcelize
data class Query(
    val familiarity: Familiarity? = null,
    val instrumental: Boolean? = null,
    val acousticness: Float? = null,
    val valence: Float? = null,
    val energy: Float? = null,
    val danceability: Float? = null,
    val genres: Set<String>? = null
) : Parcelable {
    enum class Familiarity(val stringValue: String) {
        CURRENT_FAVORITE("current favorite"),
        RELIABLE_CLASSIC("reliable classic"),
        UNDERAPPRECIATED_GEM("underappreciated gem")
    }
    enum class Parameter(val stringValue: String) {
        FAMILIARITY("familiarity"),
        INSTRUMENTALNESS("instrumentalness"),
        ACOUSTICNESS("acousticness"),
        VALENCE("valence"),
        ENERGY("energy"),
        DANCEABILITY("danceability"),
        GENRES("genres")
    }
}
