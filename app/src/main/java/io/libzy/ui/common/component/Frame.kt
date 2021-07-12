package io.libzy.ui.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A container which takes up the maximum space available and places its [content] in the center of it.
 */
@Composable
fun Frame(content: @Composable BoxScope.() -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize(), content = content)
}
