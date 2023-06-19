package io.libzy.domain

import android.os.Parcelable
import androidx.annotation.StringRes
import io.libzy.R
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
    enum class Parameter(val stringValue: String, @StringRes val labelResId: Int) {
        FAMILIARITY("familiarity", R.string.familiarity_label),
        INSTRUMENTALNESS("instrumentalness", R.string.instrumentalness_label),
        ACOUSTICNESS("acousticness", R.string.acousticness_label),
        VALENCE("valence", R.string.valence_label),
        ENERGY("energy", R.string.energy_label),
        DANCEABILITY("danceability", R.string.danceability_label),
        GENRES("genres", R.string.genres_label);

        companion object {

            val defaultOrder = Parameter.values().toList()

            fun fromString(stringValue: String) = values().find {
                it.stringValue == stringValue
            } ?: throw IllegalArgumentException("Unknown query parameter: '$stringValue'")
        }
    }
}
