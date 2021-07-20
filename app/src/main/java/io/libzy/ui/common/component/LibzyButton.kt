package io.libzy.ui.common.component

import androidx.annotation.StringRes
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun LibzyButton(@StringRes textResId: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier) {
        Text(stringResource(textResId))
    }
}
