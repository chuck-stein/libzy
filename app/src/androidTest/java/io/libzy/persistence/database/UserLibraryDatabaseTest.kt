package io.libzy.persistence.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.libzy.persistence.database.entity.DbAlbum
import io.libzy.persistence.database.entity.DbGenre
import io.libzy.persistence.database.entity.junction.AlbumGenreJunction
import io.libzy.persistence.database.tuple.AudioFeaturesTuple
import io.libzy.persistence.database.tuple.FamiliarityTuple
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

// TODO: add test methods
@RunWith(AndroidJUnit4::class)
class UserLibraryDatabaseTest {

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
    fun createDb() = runTest {
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
                AlbumGenreJunction("3", "rock"),
                AlbumGenreJunction("4", "rap")
            )
        )
    }

    @Test
    fun deleteGenresForDeletedAlbum() = runTest {
        assertTrue("rap" in db.genreDao.getAll())
        db.genreDao.deleteForDeletedAlbum(rapAlbum.spotifyId)
        assertFalse("rap" in db.genreDao.getAll())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
}
