package io.libzy.database.tuple

import androidx.room.ColumnInfo

data class FamiliarityTuple(

    @ColumnInfo(name = "recently_played")
    val recentlyPlayed: Boolean,

    @ColumnInfo(name = "short_term_favorite")
    val shortTermFavorite: Boolean,

    @ColumnInfo(name = "medium_term_favorite")
    val mediumTermFavorite: Boolean,

    @ColumnInfo(name = "long_term_favorite")
    val longTermFavorite: Boolean
) {
    fun isLowFamiliarity() = !recentlyPlayed && !shortTermFavorite && !mediumTermFavorite && !longTermFavorite
}
