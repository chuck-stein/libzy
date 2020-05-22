package com.chuckstein.libzy.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album")
data class DbAlbum (

    @PrimaryKey
    @ColumnInfo(name = "id")
    val spotifyId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artists")
    val artists: String,

    @ColumnInfo(name = "artwork_url")
    val artworkUrl: String?,

    @ColumnInfo(name = "spotify_uri")
    val spotifyUri: String

)