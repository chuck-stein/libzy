package io.libzy.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.libzy.persistence.database.entity.junction.AlbumGenreJunction

@Dao
interface AlbumGenreJunctionDao : BaseDao<AlbumGenreJunction> {

    @Query("DELETE FROM album_has_genre")
    fun deleteAll()

    @Transaction
    fun replaceAll(albumGenreJunctions: Collection<AlbumGenreJunction>) {
        deleteAll()
        insertAll(albumGenreJunctions)
    }

}
