package io.libzy.domain

import io.libzy.persistence.database.tuple.LibraryAlbum

data class AlbumResult(
    val title: String,
    val artists: String,
    val spotifyUri: String,
    val artworkUrl: String? = null
)

fun LibraryAlbum.toAlbumResult() = AlbumResult(title, artists, spotifyUri, artworkUrl)
