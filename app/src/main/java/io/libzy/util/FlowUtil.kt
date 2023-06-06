package io.libzy.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/**
 * For every iterable value produced by the underlying flow, emit each item in that iterable
 */
fun <T> Flow<Iterable<T>>.flatten() = transform { items ->
    items.forEach {
        emit(it)
    }
}