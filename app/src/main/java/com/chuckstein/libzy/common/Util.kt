package com.chuckstein.libzy.common

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.roundToInt

// TODO: organize this file

fun currentTimeSeconds() = (System.currentTimeMillis() / 1000.0).roundToInt()

val ViewGroup.children: List<View>
    get() {
        val children = mutableListOf<View>()
        for (i in 0 until childCount) {
            children.add(getChildAt(i))
        }
        return children
    }

// based on https://github.com/android/architecture-components-samples/blob/master/GithubBrowserSample/app/src/test-common/java/com/android/example/github/util/LiveDataTestUtil.kt
fun <T> LiveData<T>.getOrAwaitValue(timeoutSeconds: Long = 2): T {
    var value: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(newValue: T) {
            value = newValue
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }

    // ensure observeForever runs on main thread, otherwise it will throw exception
    Handler(Looper.getMainLooper()).post {
        this.observeForever(observer)
    }

    if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
        this.removeObserver(observer)
        throw TimeoutException("LiveData value was never set.")
    }

    @Suppress("UNCHECKED_CAST")
    return value as T
}

// TODO: get country/state/city abbreviations from library or online?
val UPPER_CASE_WORDS = setOf(
    "edm", "idm", "r&b", "opm", "usbm", "uk", "ny", "nl", "us", "nc", "tx", "nz", "dc", "fl", "kc", "okc", "rva", "ecm",
    "lds", "asmr", "lldm", "stl", "gbvfi", "uk82", "nyhc", "ukhc", "czsk", "vbs", "diy", "bc", "la", "nyc", "sf", "dfw",
    "png", "atl", "cedm", "ukg", "ebm", "gqom", "nrg", "lgbtq+", "dmv", "mpb"
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
