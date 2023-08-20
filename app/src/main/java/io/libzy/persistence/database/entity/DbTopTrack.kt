package io.libzy.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.adamratzman.spotify.endpoints.client.ClientPersonalizationApi.TimeRange


@Entity(tableName = "top_track")
data class DbTopTrack(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val spotifyId: String,

    @ColumnInfo(name = "spotify_uri")
    val spotifyUri: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artists")
    val artists: String,

    @ColumnInfo(name = "album_id")
    val albumId: String,

    @ColumnInfo(name = "time_range")
    val timeRange: TimeRange
)