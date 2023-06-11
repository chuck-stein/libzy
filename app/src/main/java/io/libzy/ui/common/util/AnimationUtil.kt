package io.libzy.ui.common.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * An overload for [androidx.compose.animation.AnimatedVisibility] which remembers the last value of [stateToRemember]
 * from when [visible] was true, and passes it into the given [content] while animating it out once [visible] becomes
 * false. While [visible] is true, it simply passes in the current value of [stateToRemember] to the given [content].
 *
 * This overload is useful when some state that [content] depends on
 * in order to render is not available when [visible] is false.
 */
@Composable
fun <S> StatefulAnimatedVisibility(
    visible: Boolean,
    stateToRemember: S,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    content: @Composable AnimatedVisibilityScope.(rememberedState: S) -> Unit
) {
    val rememberedState = rememberAndRecalculateIf(visible) { stateToRemember }

    AnimatedVisibility(visible, modifier, enter, exit) {
        content(rememberedState)
    }
}
