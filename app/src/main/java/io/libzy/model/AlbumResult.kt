package io.libzy.model

data class AlbumResult(
    val title: String,
    val artists: String,
    val artworkUrl: String? = null,
    val spotifyUri: String? = null
)
