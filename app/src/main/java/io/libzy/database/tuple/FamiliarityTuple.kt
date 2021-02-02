package io.libzy.database.tuple

import androidx.room.ColumnInfo

data class FamiliarityTuple(

    @ColumnInfo(name = "recently_played")
    val recentlyPlayed: Boolean,

    // TODO: is this how I want to store the data? how will I present it? A floating point "familiarity" value is another option
    @ColumnInfo(name = "short_term_favorite")
    val shortTermFavorite: Boolean,

    // TODO: is this how I want to store the data? how will I present it? A floating point "familiarity" value is another option
    @ColumnInfo(name = "medium_term_favorite")
    val mediumTermFavorite: Boolean,

    // TODO: is this how I want to store the data? how will I present it? A floating point "familiarity" value is another option
    @ColumnInfo(name = "long_term_favorite")
    val longTermFavorite: Boolean
) {
    fun isLowFamiliarity() = !recentlyPlayed && !shortTermFavorite && !mediumTermFavorite && !longTermFavorite
}