package io.libzy.domain

/**
 * A group of recommended [AlbumResult]s with shared characteristic(s) indicated by a [title].
 */
data class RecommendationCategory(
    val title: String,
    val albumResults: List<AlbumResult>
)
