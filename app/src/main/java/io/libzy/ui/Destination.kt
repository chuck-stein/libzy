package io.libzy.ui

import android.net.Uri
import androidx.navigation.NavDeepLink
import androidx.navigation.compose.NamedNavArgument
import androidx.navigation.navDeepLink

/**
 * Represents a screen in the navigation graph, with a corresponding route, arguments, and deep links.
 */
sealed class Destination {
    abstract val route: String
    open val arguments: List<NamedNavArgument> = emptyList()
    open val deepLinks: List<NavDeepLink> = emptyList()
    open val requiresSpotifyConnection = true

    protected fun createDeepLinkUri(): Uri = Uri.Builder()
        .scheme("libzy")
        .authority(route)
        .build()

    protected fun createDeepLinksFrom(vararg deepLinkUris: Uri) = deepLinkUris.map {
        navDeepLink {
            uriPattern = it.toString()
        }
    }

    object NavHost : Destination() {
        override val route = "host"
    }
    object ConnectSpotify : Destination() {
        override val route = "connect"
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
        override val requiresSpotifyConnection = false
    }
    object FindAlbumFlow : Destination() {
        override val route = "find-album"
    }
    object Query : Destination() {
        override val route = "query"
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }
    object Results : Destination() {
        override val route = "results"
    }
}
