package io.libzy.persistence.database.tuple

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import io.libzy.persistence.database.entity.DbGenre
import io.libzy.persistence.database.entity.junction.AlbumGenreJunction

// TODO: remove any unused columns
data class LibraryAlbum(

    @ColumnInfo(name = "id")
    val spotifyId: String,
    
    @ColumnInfo(name = "spotify_uri")
    val spotifyUri: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artists")
    val artists: String,

    @ColumnInfo(name = "artwork_url")
    val artworkUrl: String?,

    @ColumnInfo(name = "year_released")
    val yearReleased: Int,

    @ColumnInfo(name = "popularity")
    val popularity: Float,

    @Embedded
    val audioFeatures: AudioFeaturesTuple,

    @Embedded
    val familiarity: FamiliarityTuple,

    @Relation(
        parentColumn = "id",
        entityColumn = "name_id",
        entity = DbGenre::class,
        associateBy = Junction(
            value = AlbumGenreJunction::class,
            parentColumn = "album_id",
            entityColumn = "genre_id"
        ),
        projection = ["name_id"]
    )
    val genres: Set<String>
    
)
