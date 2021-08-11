package io.libzy.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * Contains the colors associated with Libzy's [MaterialTheme] implementation,
 * as well as any other colors used in the app's Compose UI.
 *
 * If a color defined both here and in colors.xml is to change,
 * be sure to change it in both places.
 */
object LibzyColors {

    // Material Theme Colors
    val Purple = Color(0xFF7D3C96)
    val DarkPurple = Color(0xFF462055)
    val PinkGray = Color(0xFF7B5B86)
    val LightPink = Color(0xFFF7C1BB)
    val OffWhite = Color(0xFFCACACA)

    // Background Gradient Colors
    val DeepPurple = Color(0xFF0E0023)
    val VeryDeepPurple = Color(0xFF05000E)

    // Other Colors (use sparingly)
    val Gray = Color(0xFF808080)

    /**
     * Some external APIs do not accept true transparency (i.e. [Color.Transparent]) as a [Color] parameter.
     * For example, Accompanist's System UI Controller will instead use a 50% alpha black color if this is attempted
     * on the system nav bar. [ForcedTransparency] is a workaround for such scenarios, providing a 1% alpha black color
     * which is not replaced with a different color because it is not truly transparent, while still appearing so.
     */
    val ForcedTransparency = Color.Black.copy(alpha = 0.01f)
}
