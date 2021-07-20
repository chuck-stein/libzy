package io.libzy.util

import kotlin.math.roundToInt

// NOTE: after January 18th, 2038, this function will need to change because it will only return Int.MAX_VALUE
fun currentTimeSeconds() = (System.currentTimeMillis() / 1000.0).roundToInt()

fun percentageToFloat(percentage: Int) = percentage / 100F

/**
 * Formats this Float as a string with precision up to the given number of decimal places.
 */
fun Float.toString(decimalPlaces: Int): String {
    return "%.${decimalPlaces}f".format(this)
}
