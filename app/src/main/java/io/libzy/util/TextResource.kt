package io.libzy.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface TextResource {
    data class Id(@StringRes val resId: Int) : TextResource
    data class Value(val text: String) : TextResource
}

fun Int.toTextResource() = TextResource.Id(this)
fun String.toTextResource() = TextResource.Value(this)

@Composable
fun TextResource.resolveText() = when (this) {
    is TextResource.Id -> stringResource(resId)
    is TextResource.Value -> text
}

val emptyTextResource = TextResource.Value("")
