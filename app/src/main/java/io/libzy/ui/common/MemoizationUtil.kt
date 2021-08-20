package io.libzy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
inline fun <T> rememberAndRecalculateIf(predicate: Boolean, calculation: @DisallowComposableCalls () -> T): T {
    var recalculationKey by remember { mutableStateOf(false) }
    if (predicate) {
        // changing the key means we should recalculate, and we only change it if the predicate is true
        recalculationKey = !recalculationKey
    }
    return remember(recalculationKey, calculation)
}
