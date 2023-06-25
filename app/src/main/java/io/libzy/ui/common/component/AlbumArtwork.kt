package io.libzy.ui.common.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.libzy.R
import io.libzy.ui.common.util.loadRemoteImage

@Composable
fun AlbumArtwork(artworkUrl: String?, modifier: Modifier = Modifier) {
    val artworkContentDescription = stringResource(R.string.cd_album_artwork)

    val artworkBitmap = loadRemoteImage(artworkUrl)
    if (artworkBitmap != null) {
        Image(artworkBitmap, artworkContentDescription, modifier)
    } else {
        Image(
            painterResource(R.drawable.placeholder_album_art),
            artworkContentDescription,
            modifier,
            contentScale = ContentScale.FillWidth
        )
    }
}