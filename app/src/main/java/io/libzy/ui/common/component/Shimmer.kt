package io.libzy.ui.common.component

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

@Composable
fun Shimmer(
    animationDurationMillis: Int,
    shimmerColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val gradientPositionOffset by rememberInfiniteTransition(label = "shimmer transition").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer offset"
    )

    // negative color stops begin offscreen, then animate onscreen
    // as the animation progresses, for an infinite ripple/shimmer effect
    val gradient = Brush.linearGradient(
        -1f + gradientPositionOffset to backgroundColor,
        -0.5f + gradientPositionOffset to shimmerColor,
        0f + gradientPositionOffset to backgroundColor,
        0.5f + gradientPositionOffset to shimmerColor,
        1f + gradientPositionOffset to backgroundColor
    )
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .then(modifier),
        color = Color.Transparent,
        contentColor = Color.White,
        content = content
    )
}
