package io.libzy.persistence.database.tuple

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
): Comparable<FamiliarityTuple> {

    fun isLowFamiliarity() = !recentlyPlayed && !shortTermFavorite && !mediumTermFavorite && !longTermFavorite

    private val familiarityScore
        get() = listOfNotNull(
            1.takeIf { recentlyPlayed },
            2.takeIf { shortTermFavorite },
            4.takeIf { mediumTermFavorite },
            8.takeIf { longTermFavorite }
        ).sum()

    override fun compareTo(other: FamiliarityTuple) = this.familiarityScore.compareTo(other.familiarityScore)
}
