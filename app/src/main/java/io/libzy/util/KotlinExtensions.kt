package io.libzy.util

import java.util.Locale

/**
 * Creates a new read-only map by replacing or adding entries to this map from the given key-value [pairs].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those [pairs] with unique keys are iterated in the end in the order of [pairs] collection.
 */
fun <K, V> Map<out K, V>.plus(vararg pairs: Pair<K, V>): Map<K, V> =
    if (this.isEmpty()) pairs.toMap() else LinkedHashMap(this).apply { putAll(pairs) }

/**
 * Capitalizes the first letter of each word in this String, where words are divided by spaces.
 */
fun String.capitalizeAllWords() = split(" ").joinToString(separator = " ") { lowercaseWord ->
    lowercaseWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

/**
 * Join a collection of Strings into a single string, using ", " as a separator, except for the last separator,
 * which uses " & ". This has a more user friendly appearance for a list, e.g. "one, two, three & four".
 */
fun Collection<String>.joinToUserFriendlyString() = when (size) {
    0 -> ""
    1 -> first()
    else -> take(size - 1).joinToString() + " & ${last()}"
}
