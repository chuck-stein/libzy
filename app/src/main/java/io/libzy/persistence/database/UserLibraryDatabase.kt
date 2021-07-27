package io.libzy.persistence.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.libzy.persistence.database.dao.AlbumDao
import io.libzy.persistence.database.dao.AlbumGenreJunctionDao
import io.libzy.persistence.database.dao.GenreDao
import io.libzy.persistence.database.entity.DbAlbum
import io.libzy.persistence.database.entity.DbGenre
import io.libzy.persistence.database.entity.junction.AlbumGenreJunction

@Database(entities = [DbAlbum::class, DbGenre::class, AlbumGenreJunction::class], version = 1)
abstract class UserLibraryDatabase : RoomDatabase() {

    abstract val albumDao: AlbumDao
    abstract val genreDao: GenreDao
    abstract val albumGenreJunctionDao: AlbumGenreJunctionDao

}
