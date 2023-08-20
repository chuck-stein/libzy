package io.libzy.ui.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.libzy.ui.common.util.hideIf
import io.libzy.ui.theme.LibzyDimens.CIRCULAR_PROGRESS_INDICATOR_SIZE

/**
 * Display the given [content] once [loading] becomes false.
 *
 * @param enableProgressIndicator Whether we should show a progress indicator while [loading].
 * Set to false to avoid flashing the indicator for a split second if the loading time should not be noticeable.
 */
@Composable
fun LoadedContent(loading: Boolean, enableProgressIndicator: Boolean = true, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.hideIf(loading)) {
            content()
        }
        CircularProgressIndicator(
            modifier = Modifier
                .size(CIRCULAR_PROGRESS_INDICATOR_SIZE.dp)
                .hideIf(!loading || !enableProgressIndicator)
        )
    }
}