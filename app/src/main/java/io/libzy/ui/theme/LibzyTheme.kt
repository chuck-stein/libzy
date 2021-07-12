package io.libzy.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.libzy.R

/**
 * Libzy's app-wide implementation of [MaterialTheme].
 */
@Composable
fun LibzyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = DarkColors, typography = LibzyTypography, content = content)
}

private val DarkColors = darkColors(
    primary = LibzyColors.Purple,
    primaryVariant = LibzyColors.Violet,
    secondary = LibzyColors.Melon,
    secondaryVariant = LibzyColors.LightPink,
    error = Color.Red,
    surface = LibzyColors.DarkGray,
    onSurface = Color.White
)

private val LibzyTypography = Typography(
    defaultFontFamily = FontFamily(
        Font(R.font.varela_round_regular),
        Font(R.font.varela_round_regular, FontWeight.Bold)
    ),
    h1 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 60.sp,
        textAlign = TextAlign.Center,
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        textAlign = TextAlign.Center,
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        textAlign = TextAlign.Center,
    ),
    h4 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        textAlign = TextAlign.Center,
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
    ),
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        textAlign = TextAlign.Center
    ),
    body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
)
