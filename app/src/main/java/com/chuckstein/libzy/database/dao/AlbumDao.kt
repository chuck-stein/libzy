package com.chuckstein.libzy.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.chuckstein.libzy.database.entity.DbAlbum
import com.chuckstein.libzy.database.tuple.AudioFeaturesTuple
import com.chuckstein.libzy.database.tuple.LibraryAlbum

@Dao
interface AlbumDao : BaseDao<DbAlbum> {

    @Query("DELETE FROM album")
    fun deleteAll()

    @Transaction
    fun replaceAll(albums: Collection<DbAlbum>) {
        deleteAll()
        insertAll(albums)
    }

    @Query("SELECT * FROM album")
    fun getAllAlbums(): LiveData<List<LibraryAlbum>>

    @Query("SELECT * FROM album WHERE spotify_uri = :uri")
    suspend fun getAlbumFromUri(uri: String): LibraryAlbum

    @Query(
        """
        SELECT valence, acousticness, instrumentalness, energy, danceability, liveness
        FROM album
        WHERE id = :albumId
        """
    )
    fun getAudioFeatures(albumId: String): AudioFeaturesTuple?

}