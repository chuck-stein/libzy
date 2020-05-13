package com.chuckstein.libzy.view.browseresults.data

data class AlbumResult(
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val spotifyUri: String // TODO: does putting the Spotify URI in the object used for view display violate separation of concerns? also, can it be a Uri object?
)