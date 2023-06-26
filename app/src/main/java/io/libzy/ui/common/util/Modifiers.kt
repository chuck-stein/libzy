package io.libzy.ui.common.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

fun Modifier.hideIf(shouldHide: Boolean) = alpha(if (shouldHide) 0f else 1f)

fun Modifier.fadingEdge(brush: ContentDrawScope.() -> Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush(), blendMode = BlendMode.DstIn)
    }

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.fadingMarquee() = fadingEdge {
    Brush.horizontalGradient(
        0f to Color.Transparent,
        0.1f to Color.Black,
        0.9f to Color.Black,
        1f to Color.Transparent
    )
}.basicMarquee(iterations = Int.MAX_VALUE, delayMillis = 0, spacing = { _, _ -> 0 })

@Composable
fun Modifier.surfaceBackground() = background(MaterialTheme.colors.surface.copy(alpha = 0.7f), RoundedCornerShape(8.dp))