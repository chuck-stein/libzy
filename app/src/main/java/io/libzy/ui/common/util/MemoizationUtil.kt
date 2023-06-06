package io.libzy.ui.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Return and remember the value produced by [calculation]. Subsequent recompositions will return the same remembered
 * value, unless the given [predicate] is true, in which case produce and remember a new value by calling [calculation].
 */
@Composable
inline fun <T> rememberAndRecalculateIf(predicate: Boolean, crossinline calculation: @DisallowComposableCalls () -> T): T {
    var recalculationKey by remember { mutableStateOf(false) }
    if (predicate) {
        // changing the key means we should recalculate, and we only change it if the predicate is true
        recalculationKey = !recalculationKey
    }
    return remember(recalculationKey, calculation)
}
