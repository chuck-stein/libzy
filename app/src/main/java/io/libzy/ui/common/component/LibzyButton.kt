package io.libzy.ui.common.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun LibzyButton(
    @StringRes textResId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    startContent: @Composable RowScope.() -> Unit = {},
    endContent: @Composable RowScope.() -> Unit = {}
) {
    Button(onClick, modifier) {
        startContent()
        Text(stringResource(textResId))
        endContent()
    }
}
