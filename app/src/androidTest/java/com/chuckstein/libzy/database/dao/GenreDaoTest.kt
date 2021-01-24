package com.chuckstein.libzy.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chuckstein.libzy.database.UserLibraryDatabase
import com.chuckstein.libzy.database.entity.DbAlbum
import com.chuckstein.libzy.database.entity.DbGenre
import com.chuckstein.libzy.database.entity.junction.AlbumGenreJunction
import com.chuckstein.libzy.database.tuple.AudioFeaturesTuple
import com.chuckstein.libzy.database.tuple.FamiliarityTuple
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

// TODO: make a base db test class that initializes the database and test data
@RunWith(AndroidJUnit4::class)
class GenreDaoTest {

    private lateinit var db: UserLibraryDatabase

    private val testAudioFeatures = AudioFeaturesTuple(0.3f, 0.7f, 0.1f, 0.2f, 0.5f, 0.9f)
    private val metalAlbum = DbAlbum(
        "1", "Lateralus", "Tool", "lateralus-artwork.com", "lateralus-uri", 2001, 0.6f, testAudioFeatures,
        FamiliarityTuple(
            recentlyPlayed = false,
            shortTermFavorite = false,
            mediumTermFavorite = false,
            longTermFavorite = true
        )

    )
    private val rockAndMetalAlbum = DbAlbum(
        "2", "Blast Tyrant", "Clutch", "blast-tyrant-artwork.com", "blast-tyrant-uri", 2004, 0.4f, testAudioFeatures,
        FamiliarityTuple(
            recentlyPlayed = false,
            shortTermFavorite = false,
            mediumTermFavorite = false,
            longTermFavorite = true
        )
    )
    private val rockAlbum = DbAlbum(
        "3", "Houses of the Holy", "Led Zeppelin", "hoth-artwork.com", "hoth-uri", 1973, 0.8f, testAudioFeatures,
        FamiliarityTuple(
            recentlyPlayed = false,
            shortTermFavorite = false,
            mediumTermFavorite = false,
            longTermFavorite = true
        )
    )
    private val rapAlbum = DbAlbum(
        "4", "Yeezus", "Kanye West", "yeezus-artwork.com", "yeezus-uri", 2013, 0.9f, testAudioFeatures,
        FamiliarityTuple(
            recentlyPlayed = false,
            shortTermFavorite = false,
            mediumTermFavorite = false,
            longTermFavorite = true
        )
    )

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
    fun testSomething() {
        // TODO
    }

}