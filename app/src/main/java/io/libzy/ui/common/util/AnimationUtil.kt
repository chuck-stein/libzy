package io.libzy.ui.common.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.with
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/**
 * An overload for [androidx.compose.animation.AnimatedContent] which transitions its content
 * only when the given [key] changes, rather than every time the given [targetState] changes.
 *
 * As with [androidx.compose.animation.AnimatedContent], the value of [targetState] from before
 * the transition will be sent to the outgoing [content], while the current value of [targetState]
 * will be sent to the incoming [content].
 *
 * This overload is useful when the state that the [content] depends on
 * differs from the state that should trigger a transition when changed.
 */
@ExperimentalAnimationApi
@Composable
fun <S> AnimatedContent(
    targetState: S,
    key: Any?,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentDelegatorScope<S>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(220, delayMillis = 90)) with fadeOut(animationSpec = tween(90))
    },
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable AnimatedVisibilityScope.(targetState: S) -> Unit
) {
    val keyedTargetState = key(key) {
        rememberUpdatedState(targetState)
    }

    AnimatedContent(
        targetState = keyedTargetState,
        modifier = modifier,
        transitionSpec = { AnimatedContentDelegatorScope(this).transitionSpec() },
        contentAlignment = contentAlignment,
        content = { content(it.value) }
    )
}

/**
 * A wrapper for an [AnimatedContentScope] that has a state of type [State]<[S]>, providing the same properties
 * and functionality as [AnimatedContentScope], but with respect to a state of type [S] (equivalent to the `value` of
 * the wrapped [State]), to hide away the detail of the actual state being wrapped in the [State] type.
 *
 * This cannot be done as an anonymous subclass of [AnimatedContentScope] that overrides [initialState] and
 * [targetState] to reflect the wrapped [AnimatedContentScope]'s [State]'s `value`, because [AnimatedContentScope]
 * is a final class.
 *
 * TODO: Once [AnimatedContentScope] is no longer experimental, see if the aforementioned alternative approach will
 *  now work, as there is a note in the source that it may become a (hopefully public) interface.
 */
@ExperimentalAnimationApi
class AnimatedContentDelegatorScope<S>(private val delegate: AnimatedContentScope<State<S>>) {

    val initialState: S
        get() = delegate.initialState.value
    val targetState: S
        get() = delegate.targetState.value

    infix fun ContentTransform.using(sizeTransform: SizeTransform?) = with(delegate) { using(sizeTransform) }

    fun slideIntoContainer(
        towards: AnimatedContentScope.SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset> = spring(
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
        initialOffset: (offsetForFullSlide: Int) -> Int = { it }
    ) = delegate.slideIntoContainer(towards, animationSpec, initialOffset)

    fun slideOutOfContainer(
        towards: AnimatedContentScope.SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset> = spring(
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
        targetOffset: (offsetForFullSlide: Int) -> Int = { it }
    ) = delegate.slideOutOfContainer(towards, animationSpec, targetOffset)
}

/**
 * An overload for [androidx.compose.animation.AnimatedVisibility] which remembers the last value of [stateToRemember]
 * from when [visible] was true, and passes it into the given [content] while animating it out once [visible] becomes
 * false. While [visible] is true, it simply passes in the current value of [stateToRemember] to the given [content].
 *
 * This overload is useful when some state that [content] depends on
 * in order to render is not available when [visible] is false.
 */
@ExperimentalAnimationApi
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
