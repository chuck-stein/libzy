package io.libzy.ui.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.libzy.ui.theme.LibzyDimens.CIRCULAR_PROGRESS_INDICATOR_SIZE

@Composable
fun LoadedContent(loading: Boolean, content: @Composable () -> Unit) {
    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(CIRCULAR_PROGRESS_INDICATOR_SIZE.dp))
        }
    } else {
        content()
    }
}