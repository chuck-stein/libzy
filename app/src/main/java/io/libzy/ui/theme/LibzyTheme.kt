package io.libzy.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.LineBreak
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
    primaryVariant = LibzyColors.DarkPurple,
    secondary = LibzyColors.PinkGray,
    secondaryVariant = LibzyColors.DarkPinkGray,
    error = Color.Red,
    surface = LibzyColors.SurfaceGray
)

private val LibzyTypography = Typography(
    defaultFontFamily = Font(R.font.varela_round).toFontFamily(),
    h1 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 60.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Heading
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Heading
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Heading
    ),
    h4 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Heading
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Heading
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Heading
    ),
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Paragraph
    ),
    body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Paragraph
    ),
    subtitle1 = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
        lineBreak = LineBreak.Paragraph
    ),
    button = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        letterSpacing = 1.1.sp
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        color = LibzyColors.Gray
    )
)
