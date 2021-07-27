package io.libzy.ui.common.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * Observes the [Lifecycle] of the current [LocalLifecycleOwner]
 * and calls the given callbacks for each corresponding lifecycle event.
 *
 * Removes the observer when this composable leaves the composition.
 */
@Composable
fun LifecycleObserver(
    onStart: () -> Unit = {},
    onStop: () -> Unit = {}
) {
    val currentLifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(currentLifecycle) {

        val lifecycleObserver = object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStartEvent() {
                onStart()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStopEvent() {
                onStop()
            }
        }

        currentLifecycle.addObserver(lifecycleObserver)

        onDispose {
            currentLifecycle.removeObserver(lifecycleObserver)
        }
    }
}
