package io.libzy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.libzy.ui.theme.LibzyColors
import io.libzy.ui.theme.LibzyTheme

/**
 * A wrapper for any and all [Composable] UI content in the app. Decorates its [content] with Libzy's Material Theme
 * implementation, the app-wide background gradient animation, a provider for window insets, and transparency for the
 * system status bar and navigation bar.
 *
 * This should go at the top of any compose tree, whether that is the actual user-facing tree defined in [MainActivity],
 * or a tree defined for a UI [Preview].
 */
@Composable
fun LibzyContent(content: @Composable () -> Unit) {
    LibzyTheme {
        ProvideWindowInsets {
            val systemUiController = rememberSystemUiController()
            SideEffect {
                systemUiController.setStatusBarColor(Color.Transparent)
                systemUiController.setNavigationBarColor(LibzyColors.ForcedTransparency)
            }
            BackgroundGradient(content)
        }
    }
}
