package io.libzy.util

/**
 * Creates a new read-only map by replacing or adding entries to this map from the given key-value [pairs].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those [pairs] with unique keys are iterated in the end in the order of [pairs] collection.
 */
fun <K, V> Map<out K, V>.plus(vararg pairs: Pair<K, V>): Map<K, V> =
    if (this.isEmpty()) pairs.toMap() else LinkedHashMap(this).apply { putAll(pairs) }
