package com.chuckstein.libzy.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Represents a user's mood / listening preferences, as a collection of desired album properties.
 * 
 * Null values indicate no preference for the associated property.
 */
@Parcelize
data class Query(
    var familiarity: Familiarity? = null,
    var instrumental: Boolean? = null,
    var acousticness: Float? = null,
    var valence: Float? = null,
    var energy: Float? = null,
    var danceability: Float? = null,
    var genres: Set<String>? = null
) : Parcelable {
    enum class Familiarity {
        CURRENT_FAVORITE, RELIABLE_CLASSIC, UNDERAPPRECIATED_GEM
    }
}
