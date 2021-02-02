package io.libzy.database.tuple

import androidx.room.ColumnInfo

data class AudioFeaturesTuple(

    @ColumnInfo(name = "valence")
    val valence: Float,

    @ColumnInfo(name = "acousticness")
    val acousticness: Float,

    @ColumnInfo(name = "instrumentalness")
    val instrumentalness: Float,

    @ColumnInfo(name = "energy")
    val energy: Float,

    @ColumnInfo(name = "danceability")
    val danceability: Float,

    @ColumnInfo(name = "liveness") // TODO: remove liveness if it's never used
    val liveness: Float

)