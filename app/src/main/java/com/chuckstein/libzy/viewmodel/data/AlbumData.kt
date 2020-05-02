package com.chuckstein.libzy.viewmodel.data

import android.net.Uri

data class AlbumData(
    val albumArtUri: Uri,
    val albumTitle: String,
    val albumArtist: String
)