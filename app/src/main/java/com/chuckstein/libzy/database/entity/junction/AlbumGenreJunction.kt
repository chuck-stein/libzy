package com.chuckstein.libzy.database.entity.junction

import androidx.room.ColumnInfo
import androidx.room.Entity

// TODO: define a foreign key relationship if/when albums and genres can be deleted discretely instead of always wiped and refreshed
@Entity(tableName = "album_has_genre", primaryKeys = ["album_id", "genre_id"])
data class AlbumGenreJunction(

    @ColumnInfo(name = "album_id")
    val albumId: String,

    @ColumnInfo(name = "genre_id")
    val genreId: String

)