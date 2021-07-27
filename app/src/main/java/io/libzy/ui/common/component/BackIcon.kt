package io.libzy.ui.common.component

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.libzy.R
import io.libzy.ui.theme.LibzyIconTheme

/**
 * A navigation icon which represents going back to previous content when clicked.
 */
@Composable
fun BackIcon(onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick, enabled = enabled) {
        Icon(
            imageVector = LibzyIconTheme.ArrowBack,
            contentDescription = stringResource(R.string.cd_navigate_back),
            tint = Color.White
        )
    }
}
