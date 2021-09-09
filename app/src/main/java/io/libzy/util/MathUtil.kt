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
