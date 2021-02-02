package io.libzy.model

data class GenreResult(
    val name: String,
    val albums: List<AlbumResult>
)