package com.chuckstein.libzy.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
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
        SELECT genre.name_id AS name, count(album_has_genre.album_id) AS num_associated_albums
        FROM genre 
        LEFT JOIN album_has_genre 
        ON genre.name_id = album_has_genre.genre_id
        GROUP BY genre.name_id
        """
    )
    fun getAllLibraryGenres(): LiveData<List<GenreWithMetadataTuple>>

    @Transaction
    @Query("SELECT name_id FROM genre WHERE name_id IN (:genres) ORDER BY name_id") // TODO: remove this "order by" when UI handles ordering
    fun getGenresWithAlbums(genres: Collection<String>): LiveData<List<GenreWithAlbumsTuple>>

}