package com.chuckstein.libzy.view.browseresults.data

import android.net.Uri

data class AlbumData(
    val title: String,
    val artist: String,
    val artworkUri: Uri,
    val spotifyUri: String // TODO: does putting the Spotify URI in the object used for view display violate separation of concerns? also, can it be a Uri object?
)