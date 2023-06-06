package io.libzy.ui.common.component

import androidx.compose.material.IconButton
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.libzy.R
import io.libzy.ui.theme.LibzyIconTheme

@Composable
fun StartOverIconButton(onClick: () -> Unit) {
    IconButton(onClick) {
        LibzyIcon(LibzyIconTheme.RestartAlt,  contentDescription = stringResource(R.string.start_over))
    }
}