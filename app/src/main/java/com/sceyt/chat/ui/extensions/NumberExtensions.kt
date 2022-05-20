package com.sceyt.chat.ui.extensions

import android.content.res.Resources
import android.util.DisplayMetrics
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

fun Double.cmToFoot(): Number {
    return (this / 30.48)
}

fun Double.footToCm(): Number {
    return (this * 30.48).roundToInt()
}

fun Number.scale(scale: Int): Double {
    return toDouble().toBigDecimal().setScale(scale, RoundingMode.UP).toDouble()
}

fun Double.getIntOrDoubleNumberValue(): Number {
    val pow10 = getPow10(toDouble())
    val myCount = (toDouble() * pow10).roundToInt() / pow10
    return if (myCount == floor(myCount) && !java.lang.Double.isInfinite(myCount)) {
        return myCount.toInt()
    } else (myCount * pow10).roundToInt() / pow10
}

private fun getPow10(skip: Double): Double {
    val value: String = skip.toString()
    return if (value.contains(".")) {
        val k = (value.length) - value.indexOf(".")
        10.0.pow(k.toDouble())
    } else 10.0
}

fun Long.toPrettySize(): String {
    val sizeInKb = (this / 1024f).toDouble()
    val sizeInMb = sizeInKb / 1024f
    val format = DecimalFormat("##.##")
    return when {
        sizeInMb >= 1 -> format.format(sizeInMb) + "MB"
        sizeInKb >= 1 -> format.format(sizeInKb) + "KB"
        else -> "${this}B"
    }
}

/**
 * Transforms DP value integer to pixels, based on the screen density.
 */
internal fun Int.dpToPx(): Int = dpToPxPrecise().roundToInt()

/**
 * Uses the display metrics to transform the value of DP to pixels.
 */
internal fun Int.dpToPxPrecise(): Float = (this * displayMetrics().density)

/**
 * Fetches the current system display metrics based on [Resources].
 */
internal fun displayMetrics(): DisplayMetrics = Resources.getSystem().displayMetrics
