package com.chuckstein.libzy.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chuckstein.libzy.database.dao.AlbumDao
import com.chuckstein.libzy.database.dao.GenreDao
import com.chuckstein.libzy.database.entity.DbAlbum
import com.chuckstein.libzy.database.entity.junction.AlbumGenreJunction
import com.chuckstein.libzy.database.dao.AlbumGenreJunctionDao
import com.chuckstein.libzy.database.entity.DbGenre

@Database(entities = [DbAlbum::class, DbGenre::class, AlbumGenreJunction::class], version = 1)
abstract class UserLibraryDatabase : RoomDatabase() {

    abstract val albumDao: AlbumDao
    abstract val genreDao: GenreDao
    abstract val albumGenreJunctionDao: AlbumGenreJunctionDao

}