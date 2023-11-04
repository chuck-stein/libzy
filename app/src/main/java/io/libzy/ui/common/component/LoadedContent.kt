package io.libzy.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.libzy.ui.common.util.hideIf
import io.libzy.ui.theme.LibzyDimens.CIRCULAR_PROGRESS_INDICATOR_SIZE
import io.libzy.util.TextResource
import io.libzy.util.resolveText

/**
 * Display the given [content] once [loading] becomes false.
 *
 * @param enableProgressIndicator Whether we should show a progress indicator while [loading].
 * Set to false to avoid flashing the indicator for a split second if the loading time should not be noticeable.
 */
@Composable
fun LoadedContent(
    loading: Boolean,
    enableProgressIndicator: Boolean = true,
    progressIndicatorCaption: TextResource? = null,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.hideIf(loading)) {
            content()
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.hideIf(!loading || !enableProgressIndicator)
        ) {
            CircularProgressIndicator(Modifier.size(CIRCULAR_PROGRESS_INDICATOR_SIZE.dp))
            if (progressIndicatorCaption != null) {
                Text(progressIndicatorCaption.resolveText(), style = MaterialTheme.typography.h6)
            }
        }
    }
}