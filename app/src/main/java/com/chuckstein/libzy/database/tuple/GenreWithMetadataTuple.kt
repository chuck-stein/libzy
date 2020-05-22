package com.chuckstein.libzy.database.tuple

import androidx.room.ColumnInfo

data class GenreWithMetadataTuple(

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "num_associated_albums")
    val numAssociatedAlbums: Int

)