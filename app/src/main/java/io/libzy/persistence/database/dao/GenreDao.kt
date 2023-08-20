package io.libzy.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.libzy.persistence.database.entity.DbGenre

@Dao
interface GenreDao : BaseDao<DbGenre> {

    @Query("DELETE FROM genre")
    suspend fun deleteAll()

    @Query("DELETE FROM genre WHERE name_id IN (SELECT genre_with_deleted_album FROM (SELECT genre_id AS genre_with_single_album FROM album_has_genre GROUP BY genre_id HAVING COUNT(album_id) = 1) JOIN (SELECT genre_id AS genre_with_deleted_album FROM album_has_genre WHERE album_id = :albumId) ON genre_with_single_album = genre_with_deleted_album)")
    suspend fun deleteForDeletedAlbum(albumId: String)

    @Query("SELECT name_id FROM genre")
    suspend fun getAll(): List<String>

    @Transaction
    suspend fun replaceAll(genres: Collection<DbGenre>) {
        deleteAll()
        insertAll(genres)
    }
}
