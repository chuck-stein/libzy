package io.libzy.ui.common.component

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Shorthand for an [Icon] with a white tint, which should be
 * the default for all Libzy icons on the dark app background.
 */
@Composable
fun LibzyIcon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier) {
    Icon(imageVector, contentDescription, modifier, tint = Color.White)
}
