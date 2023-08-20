package io.libzy.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.Closeable

/**
 * A [ViewModel] which produces [STATE] for the UI,
 * as well as instantaneous [EVENT]s for the UI to react to.
 *
 * If a screen does not require both [STATE] and [EVENT]s,
 * then instead use either [EventsOnlyViewModel] or [StateOnlyViewModel].
 *
 * @param closeables Any resources that should be closed when the ViewModel is cleared
 * @param eventBufferSize How many UI [EVENT]s should remain buffered
 *                        in memory before the event channel suspends.
 */
abstract class LibzyViewModel<STATE, EVENT>(
    vararg closeables: Closeable,
    eventBufferSize: Int = DEFAULT_EVENT_BUFFER_SIZE
) : ViewModel(*closeables) {

    protected abstract val initialUiState: STATE
    private val _uiStateFlow by lazy { MutableStateFlow(initialUiState) }
    val uiStateFlow by lazy { _uiStateFlow.asStateFlow() }
    val uiState: STATE
        get() = _uiStateFlow.value

    private val _uiEvents = Channel<EVENT>(capacity = eventBufferSize)
    val uiEvents = _uiEvents.receiveAsFlow()

    protected fun updateUiState(performUpdate: STATE.() -> STATE) {
        _uiStateFlow.update(performUpdate)
    }

    @JvmName("updateUiSubstate")
    @Suppress("UNCHECKED_CAST")
    protected fun <SUBSTATE : STATE> updateUiState(performUpdate: SUBSTATE.() -> STATE) {
        _uiStateFlow.value = (_uiStateFlow.value as? SUBSTATE)?.let(performUpdate) ?: _uiStateFlow.value
    }

    protected fun produceUiEvent(event: EVENT) {
        viewModelScope.launch {
            _uiEvents.send(event)
            Timber.d("Sent UI event: $event")
        }
    }
}

/**
 * A [ViewModel] which produces instantaneous [EVENT]s for the UI to react to, but produces no state for that UI.
 */
abstract class EventsOnlyViewModel<EVENT>(
    vararg closeables: Closeable, eventBufferSize: Int = DEFAULT_EVENT_BUFFER_SIZE
) : LibzyViewModel<Nothing, EVENT>(*closeables, eventBufferSize = eventBufferSize) {

    /** Accessing this value will throw an Error because this [EventsOnlyViewModel] holds no state. */
    override val initialUiState: Nothing
        get() = throw NotImplementedError("This ViewModel has no UI state")
}

/**
 * A [ViewModel] which produces [STATE] for the UI, but produces no instantaneous events for the UI to react to.
 */
abstract class StateOnlyViewModel<STATE>(vararg closeables: Closeable) : LibzyViewModel<STATE, Nothing>(*closeables)

private const val DEFAULT_EVENT_BUFFER_SIZE = 16
