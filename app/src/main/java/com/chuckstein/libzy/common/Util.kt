package com.chuckstein.libzy.common

import android.annotation.SuppressLint
import kotlin.math.roundToInt

fun currentTimeSeconds() = (System.currentTimeMillis() / 1000.0).roundToInt()

@SuppressLint("DefaultLocale")
fun String.capitalizeEachWord() = split(" ").joinToString(" ") { it.capitalize() }
