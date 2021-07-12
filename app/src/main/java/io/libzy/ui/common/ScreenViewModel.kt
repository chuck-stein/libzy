package io.libzy.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A [ViewModel] which produces UI [STATE] for a particular screen,
 * as well as UI [EVENT]s for that screen to react to.
 *
 * If a screen does not require both [STATE] and [EVENT]s,
 * then the unused type parameter can simply be [Nothing].
 *
 * @param eventBufferSize How many UI [EVENT]s should remain buffered
 *                        in memory before the event channel suspends.
 */
abstract class ScreenViewModel<STATE, EVENT>(eventBufferSize: Int = 16) : ViewModel() {

    protected abstract val initialUiState: STATE
    private val _uiState by lazy { MutableStateFlow(initialUiState) }
    val uiState by lazy { _uiState.asStateFlow() }

    private val _uiEvents = Channel<EVENT>(capacity = eventBufferSize)
    val uiEvents = _uiEvents.receiveAsFlow()

    protected fun updateUiState(performUpdate: STATE.() -> STATE) {
        _uiState.value = _uiState.value.performUpdate()
    }

    protected fun produceUiEvent(event: EVENT) {
        viewModelScope.launch {
            _uiEvents.send(event)
            Timber.d("Sent UI event: $event")
        }
    }
}
