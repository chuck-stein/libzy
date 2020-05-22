package com.chuckstein.libzy.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chuckstein.libzy.common.getOrAwaitValue
import com.chuckstein.libzy.database.UserLibraryDatabase
import com.chuckstein.libzy.database.entity.DbAlbum
import com.chuckstein.libzy.database.entity.DbGenre
import com.chuckstein.libzy.database.entity.junction.AlbumGenreJunction
import com.chuckstein.libzy.database.tuple.GenreWithAlbumsTuple
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

// TODO: make a base db test class that initializes the database and test data
@RunWith(AndroidJUnit4::class)
class GenreDaoTest {

    private lateinit var db: UserLibraryDatabase

    private val metalAlbum = DbAlbum("1", "Lateralus", "Tool", "lateralus-artwork.com", "lateralus-uri")
    private val rockAndMetalAlbum =
        DbAlbum("2", "Blast Tyrant", "Clutch", "blast-tyrant-artwork.com", "blast-tyrant-uri")
    private val rockAlbum = DbAlbum("3", "Houses of the Holy", "Led Zeppelin", "hoth-artwork.com", "hoth-uri")
    private val rapAlbum = DbAlbum("4", "Yeezus", "Kanye West", "yeezus-artwork.com", "yeezus-uri")

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, UserLibraryDatabase::class.java
        ).build()
        db.genreDao.insertAll(listOf("rock", "rap", "metal").map { DbGenre(it) })
        db.albumDao.insertAll(listOf(metalAlbum, rockAndMetalAlbum, rockAlbum, rapAlbum))
        db.albumGenreJunctionDao.insertAll(
            listOf(
                AlbumGenreJunction("1", "metal"),
                AlbumGenreJunction("2", "rock"),
                AlbumGenreJunction("2", "metal"),
                AlbumGenreJunction("3", "rock")
            )
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testGetGenresWithAlbums() {
        val rockAndMetalAlbums = db.genreDao.getGenresWithAlbums(listOf("rock", "metal"))
        val expectedRockAndMetalAlbums =
            listOf(
                GenreWithAlbumsTuple("metal", listOf(metalAlbum, rockAndMetalAlbum)),
                GenreWithAlbumsTuple("rock", listOf(rockAndMetalAlbum, rockAlbum))
            )
        assertEquals(expectedRockAndMetalAlbums, rockAndMetalAlbums.getOrAwaitValue())
    }

}