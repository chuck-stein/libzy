package com.chuckstein.libzy.database.tuple

import androidx.room.ColumnInfo
import androidx.room.Junction
import androidx.room.Relation
import com.chuckstein.libzy.database.entity.DbAlbum
import com.chuckstein.libzy.database.entity.junction.AlbumGenreJunction

data class GenreWithAlbumsTuple(

    @ColumnInfo(name = "name_id")
    val genre: String,

    @Relation(
        parentColumn = "name_id",
        entityColumn = "id",
        associateBy = Junction(
            value = AlbumGenreJunction::class,
            parentColumn = "genre_id",
            entityColumn = "album_id"
        )
    )
    val albums: List<DbAlbum>

)