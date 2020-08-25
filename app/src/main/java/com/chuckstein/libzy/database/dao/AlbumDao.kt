package com.chuckstein.libzy.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.chuckstein.libzy.database.entity.DbAlbum
import com.chuckstein.libzy.database.tuple.AudioFeaturesTuple

@Dao
interface AlbumDao : BaseDao<DbAlbum> {

    @Query("DELETE FROM album")
    fun deleteAll()

    @Transaction
    fun replaceAll(albums: Collection<DbAlbum>) {
        deleteAll()
        insertAll(albums)
    }

    @Query(
        """
        SELECT valence, acousticness, instrumentalness, energy, danceability, liveness
        FROM album
        WHERE id = :albumId
        """
    )
    fun getAudioFeatures(albumId: String): AudioFeaturesTuple?

    @Query(
        """
            SELECT *
            FROM album a
            JOIN album_has_genre ag
            ON a.id = ag.album_id
            WHERE ag.genre_id = :genre
        """
    )
    fun getAlbumsOfGenre(genre: String): LiveData<List<DbAlbum>>

}