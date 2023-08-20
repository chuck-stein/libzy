package io.libzy.ui.common.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import io.libzy.R
import io.libzy.util.androidAppUriFor
import io.libzy.util.isPackageInstalled

@Composable
fun OpenSpotifyButton(uri: String? = null) {
    val context = LocalContext.current
    ExtendedFloatingActionButton(
        text = { Text(stringResource(R.string.open_spotify).uppercase()) },
        onClick = { context.openSpotify(uri) },
        icon = {
            Icon(
                painterResource(R.drawable.ic_spotify_black),
                contentDescription = null
            )
        }
    )
}

private fun Context.openSpotify(specificUri: String?) {
    val spotifyIsInstalled = packageManager.isPackageInstalled(SPOTIFY_PACKAGE_NAME)

    val uri = when {
        !spotifyIsInstalled -> spotifyPlayStoreUri
        specificUri != null -> Uri.parse(specificUri)
        else -> androidAppUriFor(SPOTIFY_PACKAGE_NAME)
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        putExtra(Intent.EXTRA_REFERRER, androidAppUriFor(packageName))
    }
    ContextCompat.startActivity(this, intent, null)
}

private const val SPOTIFY_PACKAGE_NAME = "com.spotify.music"
private const val PLAY_STORE_URI = "https://play.google.com/store/apps/details"
private const val PLAY_STORE_ID_QUERY_PARAM = "id"

private val spotifyPlayStoreUri by lazy {
    Uri.parse(PLAY_STORE_URI)
        .buildUpon()
        .appendQueryParameter(PLAY_STORE_ID_QUERY_PARAM, SPOTIFY_PACKAGE_NAME)
        .build()
}