package io.libzy.domain

import com.adamratzman.spotify.models.Album
import kotlin.time.Duration.Companion.milliseconds

val Album.artworkUrl
    get() = images.firstOrNull()?.url

val Album.duration
    get() = tracks.filterNotNull().fold(initial = 0) { durationSoFar, track ->
        durationSoFar + track.durationMs
    }.milliseconds

fun Album.describe() = "${artists.joinToString { it.name }} - $name"