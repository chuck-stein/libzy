package io.libzy.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.libzy.persistence.database.tuple.AudioFeaturesTuple
import io.libzy.persistence.database.tuple.FamiliarityTuple

@Entity(tableName = "album")
data class DbAlbum(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val spotifyId: String,

    @ColumnInfo(name = "spotify_uri")
    val spotifyUri: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artists")
    val artists: String,

    @ColumnInfo(name = "artwork_url")
    val artworkUrl: String?,

    @ColumnInfo(name = "year_released")
    val yearReleased: Int,

    @ColumnInfo(name = "popularity")
    val popularity: Float,

    @Embedded
    val audioFeatures: AudioFeaturesTuple,

    @Embedded
    val familiarity: FamiliarityTuple
)
