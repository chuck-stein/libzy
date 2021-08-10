package io.libzy.ui.common.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET

@Composable
fun SelectableButton(
    @StringRes textResId: Int,
    image: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundAlpha = if (selected) 0.6f else 0.1f
    val backgroundColor = MaterialTheme.colors.primary.copy(alpha = backgroundAlpha)
    val borderColor = if (selected) Color.Transparent else MaterialTheme.colors.primary

    Button(
        onClick,
        Modifier.fillMaxWidth().padding(horizontal = HORIZONTAL_INSET.dp),
        enabled = !selected,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColorFor(backgroundColor),
            disabledBackgroundColor = backgroundColor,
            disabledContentColor = contentColorFor(backgroundColor)
        ),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Text(
            stringResource(textResId).uppercase(),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = image,
            contentDescription = null, // button text serves as adequate CD already
            modifier = Modifier.padding(horizontal = 4.dp).size(36.dp)
        )
    }
}
