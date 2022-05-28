package io.libzy.ui.common.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Collects a flow of instantaneous UI events coming from a ViewModel, so that the UI can react accordingly,
 * for example by navigating to a new screen or showing a snackbar.
 *
 * @param uiEvents The [Flow] of events to collect (each should represent a one-time event, not a persistent UI state).
 * @param eventCollector The function that will process each event.
 */
@Composable
fun <T> EventHandler(uiEvents: Flow<T>, eventCollector: FlowCollector<T>) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiEventsLifecycleAware = remember(uiEvents, lifecycleOwner) {
        uiEvents.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    LaunchedEffect(uiEventsLifecycleAware, eventCollector) {
        uiEventsLifecycleAware.onEach { Timber.d("Received UI event: $it") }.collect(eventCollector)
    }
}
