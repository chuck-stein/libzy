package io.libzy.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.libzy.persistence.database.entity.DbGenre

@Dao
interface GenreDao : BaseDao<DbGenre> {

    @Query("DELETE FROM genre")
    fun deleteAll()

    @Transaction
    fun replaceAll(genres: Collection<DbGenre>) {
        deleteAll()
        insertAll(genres)
    }

}
