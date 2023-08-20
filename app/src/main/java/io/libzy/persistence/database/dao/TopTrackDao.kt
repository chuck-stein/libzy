package io.libzy.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.libzy.persistence.database.entity.DbTopTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface TopTrackDao : BaseDao<DbTopTrack> {

    @Query("DELETE FROM top_track")
    fun deleteAll()

    @Transaction
    suspend fun replaceAll(topTracks: Collection<DbTopTrack>) {
        deleteAll()
        insertAll(topTracks)
    }

    @Query("SELECT * FROM top_track WHERE time_range = 'LongTerm'")
    fun getLongTermTopTracks(): Flow<List<DbTopTrack>>

    @Query("SELECT * FROM top_track WHERE time_range = 'MediumTerm'")
    fun getMediumTermTopTracks(): Flow<List<DbTopTrack>>

    @Query("SELECT * FROM top_track WHERE time_range = 'ShortTerm'")
    fun getShortTermTopTracks(): Flow<List<DbTopTrack>>
}