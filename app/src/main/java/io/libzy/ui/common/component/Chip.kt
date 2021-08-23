package io.libzy.ui.common.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.libzy.ui.LibzyContent

/**
 * A small, rounded, clickable surface that contains some text and can be either selected or not.
 */
@Composable
fun Chip(selected: Boolean, text: String, onClick: () -> Unit) {
    val backgroundAlpha = if (selected) 0.6f else 0.1f
    val borderStroke = if (selected) null else BorderStroke(1.dp, MaterialTheme.colors.primary)

    key(text) {
        Surface(
            color = MaterialTheme.colors.primary.copy(alpha = backgroundAlpha),
            shape = RoundedCornerShape(18.dp),
            border = borderStroke
        ) {
            Box(modifier = Modifier.clickable(onClick = onClick)) {
                Text(
                    text,
                    style = MaterialTheme.typography.body2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun UnselectedChip() {
    LibzyContent {
        Chip(selected = false, text = "unselected chip", onClick = {})
    }
}

@Preview
@Composable
private fun SelectedChip() {
    LibzyContent {
        Chip(selected = true, text = "selected chip", onClick = {})
    }
}
