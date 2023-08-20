package io.libzy.ui.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Trigger the given coroutine [block] whenever the given [lifecycleEvent] occurs.
 * When [LifecycleEffect] enters the composition, it will receive all lifecycle events up to the current state.
 * When [LifecycleEffect] leaves the composition, the coroutine [block] will be cancelled.
 */
@Composable
fun LifecycleEffect(lifecycleEvent: Lifecycle.Event, block: suspend CoroutineScope.() -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == lifecycleEvent) {
                coroutineScope.launch(block = block)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}