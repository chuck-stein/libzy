package io.libzy.ui.common.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp

@Composable
fun LibzyButton(
    @StringRes textResId: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.5f),
        disabledContentColor = contentColorFor(MaterialTheme.colors.primary).copy(alpha = 0.5f)
    ),
    startContent: (@Composable RowScope.() -> Unit)? = null,
    endContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    Button(onClick, modifier, enabled, shape = shape, border = border, colors = colors) {
        startContent?.let { content ->
            content()
            Spacer(Modifier.width(10.dp))
        }
        Text(stringResource(textResId).uppercase())
        endContent?.let { content ->
            Spacer(Modifier.width(10.dp))
            content()
        }
    }
}
