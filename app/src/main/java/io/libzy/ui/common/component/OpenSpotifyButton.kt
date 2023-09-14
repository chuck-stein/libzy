package io.libzy.ui.common.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import io.libzy.R
import io.libzy.analytics.AnalyticsConstants.EventProperties.SOURCE
import io.libzy.analytics.AnalyticsConstants.EventProperties.URI
import io.libzy.analytics.AnalyticsConstants.Events.OPEN_SPOTIFY
import io.libzy.analytics.BaseAnalyticsDispatcher
import io.libzy.analytics.LocalAnalytics
import io.libzy.ui.Destination
import io.libzy.ui.theme.LibzyColors
import io.libzy.util.androidAppUriFor
import io.libzy.util.isPackageInstalled

/**
 * A floating action button that opens Spotify when clicked.
 *
 * @param uri A specific Spotify URI to open, or null if the destination within Spotify does not matter
 * @param source The screen that the button is on, used for analytics
 */
@Composable
fun OpenSpotifyButton(uri: String? = null, source: Destination) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current

    ExtendedFloatingActionButton(
        text = { Text(stringResource(R.string.open_spotify).uppercase()) },
        icon = {
            Icon(
                painterResource(R.drawable.ic_spotify_black),
                contentDescription = null
            )
        },
        onClick = {
            context.openSpotify(source, analytics, uri)
        }
    )
}

@Composable
fun SpotifyIconButton(source: Destination, tint: Color = LibzyColors.Gray) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current

    IconButton(
        onClick = {
            context.openSpotify(source, analytics)
        }
    ) {
        LibzyIcon(
            painter = painterResource(R.drawable.ic_spotify_black),
            tint = tint,
            contentDescription = stringResource(R.string.open_spotify)
        )
    }
}

fun Context.openSpotify(source: Destination, analytics: BaseAnalyticsDispatcher, specificUri: String? = null) {
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

    analytics.sendEvent(
        eventName = OPEN_SPOTIFY,
        eventProperties = mapOf(SOURCE to source.name, URI to specificUri)
    )
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