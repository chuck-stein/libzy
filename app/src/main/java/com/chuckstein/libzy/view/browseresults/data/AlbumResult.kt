package com.chuckstein.libzy.view.browseresults.data

data class AlbumResult(
    val title: String,
    val artist: String,
    val artworkUri: String? = null,
    val spotifyUri: String? = null, // TODO: does putting the Spotify URI in the object used for view display violate separation of concerns? or is it ok because it's relevant to the click handler?
    val isPlaceholder: Boolean = false
)