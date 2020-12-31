package com.chuckstein.libzy.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.chuckstein.libzy.database.entity.DbGenre
import com.chuckstein.libzy.database.tuple.GenreWithAlbumsTuple
import com.chuckstein.libzy.database.tuple.GenreWithMetadataTuple

@Dao
interface GenreDao : BaseDao<DbGenre> {

    @Query("DELETE FROM genre")
    fun deleteAll()

    @Transaction
    fun replaceAll(genres: Collection<DbGenre>) {
        deleteAll()
        insertAll(genres)
    }

    @Query(
        """
        SELECT g.name_id AS name, count(ag.album_id) AS num_associated_albums
        FROM genre g
        LEFT JOIN album_has_genre ag
        ON g.name_id = ag.genre_id
        GROUP BY g.name_id
        """
    )
    fun getAllLibraryGenres(): LiveData<List<GenreWithMetadataTuple>>

    // TODO: delete if unused
    @Transaction
    @Query("SELECT name_id FROM genre WHERE name_id IN (:genres) ORDER BY name_id") // TODO: remove this "order by" when UI handles ordering
    fun getGenresWithAlbums(genres: Collection<String>): LiveData<List<GenreWithAlbumsTuple>>

}