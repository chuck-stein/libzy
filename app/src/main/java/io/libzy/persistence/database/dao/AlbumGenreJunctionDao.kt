package io.libzy.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.libzy.persistence.database.entity.junction.AlbumGenreJunction
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumGenreJunctionDao : BaseDao<AlbumGenreJunction> {

    @Query("DELETE FROM album_has_genre")
    fun deleteAll()

    @Query("SELECT genre_id FROM album_has_genre GROUP BY genre_id ORDER BY COUNT(album_id) DESC")
    fun getAllGenresSortedByNumAssociatedAlbums(): Flow<List<String>>

    @Transaction
    fun replaceAll(albumGenreJunctions: Collection<AlbumGenreJunction>) {
        deleteAll()
        insertAll(albumGenreJunctions)
    }

}
