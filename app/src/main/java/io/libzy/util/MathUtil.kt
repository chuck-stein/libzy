package io.libzy.util

import kotlin.math.roundToLong

fun currentTimeSeconds() = (System.currentTimeMillis() / 1000.0).roundToLong()

fun percentageToFloat(percentage: Int) = percentage / 100F

/**
 * Formats this Float as a string with precision up to the given number of decimal places.
 */
fun Float.toString(decimalPlaces: Int): String {
    return "%.${decimalPlaces}f".format(this)
}

/**
 * Calculates the [Set] of all possible combinations of [combinationSize] items from this [Collection],
 * where each distinct combination is its own [Set].
 */
fun <T> Collection<T>.combinationsOfSize(combinationSize: Int): Set<Set<T>> {
    return when (combinationSize) {
        0 -> setOf(emptySet())
        1 -> map { setOf(it) }.toSet()
        in 2..size -> {
            // Use generative recursion.
            // Choose the leftmost item. Add it to each combination of items to the right of it of size one smaller.
            // Repeat with the next leftmost item, until there are less than combinationSize items left.
            // This is analogous to how one would determine all combinations of items manually.
            val fullList = toList()
            val numLeadingItems = size - combinationSize + 1
            asSequence()
                .take(numLeadingItems)
                .withIndex()
                .map { (leadingItemIndex, leadingItem) ->
                    fullList.subList(leadingItemIndex + 1, fullList.size)
                        .combinationsOfSize(combinationSize - 1)
                        .map { childCombination -> childCombination + leadingItem }
                }
                .flatten()
                .toSet()
        }
        else -> emptySet()
    }
}
