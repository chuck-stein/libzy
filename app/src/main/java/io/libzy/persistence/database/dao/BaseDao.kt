package io.libzy.persistence.database.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy

interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: Collection<T>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: T)
}
