package com.chuckstein.libzy.common

import android.annotation.SuppressLint
import kotlin.math.roundToInt

fun currentTimeSeconds() = (System.currentTimeMillis() / 1000.0).roundToInt()


// TODO: get country/state/city abbreviations from library or online?
val UPPER_CASE_WORDS = setOf(
    "edm", "idm", "r&b", "opm", "usbm", "uk", "ny", "nl", "us", "nc", "tx", "nz", "dc", "fl", "kc", "okc", "rva", "ecm",
    "lds", "asmr", "lldm", "stl", "gbvfi", "uk82", "nyhc", "ukhc", "czsk", "vbs", "diy", "bc", "la", "nyc", "sf", "dfw",
    "png", "atl", "cedm", "ukg", "ebm", "gqom", "nrg", "lgbtq+", "dmv"
)

val LOWER_CASE_WORDS = setOf("n", "y", "and")

@SuppressLint("DefaultLocale")
fun String.capitalizeAsHeading(): String = toLowerCase().split(" ").joinToString(" ") { word ->
    word.split("-").joinToString("-") { wordPart ->
        when (wordPart) {
            in UPPER_CASE_WORDS -> wordPart.toUpperCase()
            in LOWER_CASE_WORDS -> wordPart // input already converted to lower case
            else -> wordPart.capitalize()
        }
    }
}
