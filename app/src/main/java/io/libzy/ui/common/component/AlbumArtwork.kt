package io.libzy.ui.common.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.libzy.R
import io.libzy.ui.common.util.loadRemoteImage

@Composable
fun AlbumArtwork(artworkUrl: String?, modifier: Modifier = Modifier, placeholderShimmer: Boolean = false) {
    val artworkContentDescription = stringResource(R.string.cd_album_artwork)
    val artworkModifier = modifier.aspectRatio(1f)

    val artworkBitmap = loadRemoteImage(artworkUrl)
    if (artworkBitmap != null) {
        Image(artworkBitmap, artworkContentDescription, artworkModifier)
    } else {
        Box {
            Image(
                painterResource(R.drawable.placeholder_album_art),
                artworkContentDescription,
                artworkModifier,
                contentScale = ContentScale.FillWidth
            )
            if (placeholderShimmer) {
                Shimmer(
                    animationDurationMillis = 1200,
                    shimmerColor = Color.White.copy(alpha = 0.2f),
                    backgroundColor = Color.Transparent,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}