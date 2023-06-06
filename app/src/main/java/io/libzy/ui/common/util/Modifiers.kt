package io.libzy.ui.common.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

fun Modifier.hideIf(shouldHide: Boolean) = alpha(if (shouldHide) 0f else 1f)