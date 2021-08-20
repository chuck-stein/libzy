package io.libzy.ui.common.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource

@Composable
fun LibzyButton(
    @StringRes textResId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.5f),
        disabledContentColor = contentColorFor(MaterialTheme.colors.primary).copy(alpha = 0.5f)
    ),
    startContent: @Composable RowScope.() -> Unit = {},
    endContent: @Composable RowScope.() -> Unit = {}
) {
    Button(onClick, modifier, enabled, shape = shape, border = border, colors = colors) {
        startContent()
        Text(stringResource(textResId).uppercase())
        endContent()
    }
}
