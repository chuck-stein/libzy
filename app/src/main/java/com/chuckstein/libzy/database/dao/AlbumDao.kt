package com.chuckstein.libzy.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.chuckstein.libzy.database.entity.DbAlbum

@Dao
interface AlbumDao : BaseDao<DbAlbum> {

    @Query("DELETE FROM album")
    fun deleteAll()

    @Transaction
    fun replaceAll(albums: Collection<DbAlbum>) {
        deleteAll()
        insertAll(albums)
    }

}