package io.libzy.ui

import android.net.Uri
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink

/**
 * Represents a screen in the navigation graph, with a corresponding route, arguments, and deep links.
 */
sealed class Destination(
    val name: String,
    private val requiredArguments: List<NamedNavArgument> = emptyList(),
    private val optionalArguments: List<NamedNavArgument> = emptyList(),
    val requiresSpotifyConnection: Boolean = true,
    val requiresEnoughAlbumsSaved: Boolean = true,
    val requiresOnboarding: Boolean = true
) {
    val arguments = requiredArguments + optionalArguments
    open val deepLinks: List<NavDeepLink> = emptyList()

    /** Defines the route for identifying this destination and the arguments that it accepts. */
    val route = buildString {
        append(name)
        requiredArguments.forEach { arg ->
            append("/{${arg.name}}")
        }
        optionalArguments.forEachIndexed { index, arg ->
            if (index == 0) {
                append("?")
            } else {
                append("&")
            }
            append("${arg.name}={${arg.name}}")
        }
    }

    protected fun createDeepLinkUri(): Uri = Uri.Builder()
        .scheme("libzy")
        .authority(name)
        .build()

    protected fun createDeepLinksFrom(vararg deepLinkUris: Uri) = deepLinkUris.map {
        navDeepLink {
            uriPattern = it.toString()
        }
    }

    object NavHost : Destination(name = "host")

    object ConnectSpotify : Destination(
        name = "connect",
        requiresSpotifyConnection = false,
        requiresEnoughAlbumsSaved = false,
        requiresOnboarding = false
    ) {
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }

    object ExpandLibrary : Destination(
        name = "expandLibrary",
        requiresEnoughAlbumsSaved = false,
        requiresOnboarding = false
    )

    object Onboarding : Destination(
        name = "onboarding",
        requiresOnboarding = false
    )

    object FindAlbumFlow : Destination(name = "findAlbum")

    object Query : Destination(name = "query") {
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }

    object Results : Destination(name = "results")

    object Settings : Destination(name = "settings") {
        val deepLinkUri = createDeepLinkUri()
        override val deepLinks = createDeepLinksFrom(deepLinkUri)
    }
}
