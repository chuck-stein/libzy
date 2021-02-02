package io.libzy.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.libzy.database.dao.AlbumDao
import io.libzy.database.dao.AlbumGenreJunctionDao
import io.libzy.database.dao.GenreDao
import io.libzy.database.entity.DbAlbum
import io.libzy.database.entity.DbGenre
import io.libzy.database.entity.junction.AlbumGenreJunction

@Database(entities = [DbAlbum::class, DbGenre::class, AlbumGenreJunction::class], version = 1)
abstract class UserLibraryDatabase : RoomDatabase() {

    abstract val albumDao: AlbumDao
    abstract val genreDao: GenreDao
    abstract val albumGenreJunctionDao: AlbumGenreJunctionDao

}