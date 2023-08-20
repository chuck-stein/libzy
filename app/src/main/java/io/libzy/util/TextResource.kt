package io.libzy.util

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Presentation-layer representation of text that will be displayed on the UI.
 * Call [resolveText] to convert to a string.
 */
sealed interface TextResource {

    /** Text derived from a string resource ID */
    data class Id(@StringRes val resId: Int, val formatArgs: List<Any> = emptyList()) : TextResource {
        constructor(@StringRes resId: Int, formatArg: Any) : this(resId, listOf(formatArg))
    }

    /** Text derived from a specific string */
    data class Value(val text: String) : TextResource

    /** Text derived from a plural resource ID */
    data class Plural(
        @PluralsRes val resId: Int,
        val quantity: Int,
        val formatArgs: List<Any> = emptyList()
    ) : TextResource {
        constructor(@PluralsRes resId: Int, quantity: Int, formatArg: Any) : this(resId, quantity, listOf(formatArg))
    }

    /**
     * Text derived from a specific string, with other [TextResource]s inserted into that string as format args.
     * The given string should contain a placeholder symbol for each [TextResource] argument, in the format `"%i"`,
     * where `i` is the index of the [TextResource] within the given list that should be placed there.
     *
     * For example, the string `"%0 & %1 (%2)"` will become `"Alice & Bob (feat. Jim)"` with the following [formatArgs]:
     * ```
     * listOf(TextResource.Value("Alice"), TextResource.Value("Bob"), TextResource.Value("feat. Jim"))
     * ```
     */
    data class Composite(val formattedText: String, val formatArgs: List<TextResource>) : TextResource
}

fun Int.toTextResource() = TextResource.Id(this)
fun String.toTextResource() = TextResource.Value(this)

@Composable
fun TextResource.resolveText(): String = when (this) {
    is TextResource.Id -> stringResource(resId, *formatArgs.toTypedArray())
    is TextResource.Value -> text
    is TextResource.Plural -> pluralStringResource(resId, quantity, *formatArgs.toTypedArray())
    is TextResource.Composite -> resolveCompositeText(formatArgs.map { it.resolveText() })
}

fun TextResource.resolveText(resources: Resources): String = when (this) {
    is TextResource.Id -> resources.getString(resId, *formatArgs.toTypedArray())
    is TextResource.Value -> text
    is TextResource.Plural -> resources.getQuantityString(resId, quantity, *formatArgs.toTypedArray())
    is TextResource.Composite -> resolveCompositeText(formatArgs.map { it.resolveText(resources) })
}

private fun TextResource.Composite.resolveCompositeText(formatArgs: List<String>): String {
    var compositeText = formattedText
    formatArgs.forEachIndexed { index, arg ->
        compositeText = compositeText.replace("%$index", arg)
    }
    return compositeText
}

val emptyTextResource = TextResource.Value("")
