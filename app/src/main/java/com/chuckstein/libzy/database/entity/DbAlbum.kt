package com.chuckstein.libzy.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chuckstein.libzy.database.tuple.AudioFeaturesTuple

@Entity(tableName = "album")
data class DbAlbum (

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

    @ColumnInfo(name = "recently_played")
    val recentlyPlayed: Boolean,

    // TODO: separate these to an embedded "favoriteStatus" or "affinity" or "familiarity" tuple, depending on how the data will be used

    // TODO: is this how I want to store the data? how will I present it? A floating point "familiarity" value is another option
    @ColumnInfo(name = "short_term_favorite")
    val shortTermFavorite: Boolean,

    // TODO: is this how I want to store the data? how will I present it? A floating point "familiarity" value is another option
    @ColumnInfo(name = "medium_term_favorite")
    val mediumTermFavorite: Boolean,

    // TODO: is this how I want to store the data? how will I present it? A floating point "familiarity" value is another option
    @ColumnInfo(name = "long_term_favorite")
    val longTermFavorite: Boolean

)