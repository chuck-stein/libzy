package io.libzy.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import io.libzy.ui.theme.LibzyColors

private const val GRADIENT_ANIMATION_DURATION_MILLIS = 18000

@Composable
fun BackgroundGradient(content: @Composable () -> Unit = {}) {
    val gradientPositionOffset by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(GRADIENT_ANIMATION_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // negative color stops begin offscreen, then animate onscreen
    // as the animation progresses, for an infinite ripple/shimmer effect
    val gradient = Brush.linearGradient(
        -1f + gradientPositionOffset to LibzyColors.VeryDarkPurple,
        -0.5f + gradientPositionOffset to LibzyColors.DarkPurple,
        0f + gradientPositionOffset to LibzyColors.VeryDarkPurple,
        0.5f + gradientPositionOffset to LibzyColors.DarkPurple,
        1f + gradientPositionOffset to LibzyColors.VeryDarkPurple
    )
    Surface(
        modifier = Modifier.fillMaxSize().background(gradient),
        color = Color.Transparent,
        contentColor = Color.White,
        content = content
    )
}

@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun GradientPreview() {
    BackgroundGradient()
}
