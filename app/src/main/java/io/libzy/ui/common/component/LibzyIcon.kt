package io.libzy.ui.common.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Shorthand for a 24dp [Icon] with a white tint, which should be
 * the default for all Libzy icons on the dark app background.
 */
@Composable
fun LibzyIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Icon(imageVector, contentDescription, modifier.size(24.dp), tint)
}

/**
 * Shorthand for a 24dp [Icon] with a white tint, which should be
 * the default for all Libzy icons on the dark app background.
 */
@Composable
fun LibzyIcon(
    painter: Painter,
    contentDescription: String?,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Icon(painter, contentDescription, modifier.size(24.dp), tint)
}
