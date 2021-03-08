package io.libzy.util

import kotlin.math.roundToInt

// NOTE: after January 18th, 2038, this function will need to change because it will only return Int.MAX_VALUE
fun currentTimeSeconds() = (System.currentTimeMillis() / 1000.0).roundToInt()

fun percentageToFloat(percentage: Int) = percentage / 100F
