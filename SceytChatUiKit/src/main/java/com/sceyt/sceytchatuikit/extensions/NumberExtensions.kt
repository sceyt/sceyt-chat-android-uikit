package com.sceyt.sceytchatuikit.extensions

import android.content.res.Resources
import android.util.DisplayMetrics
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

fun Double.cmToFoot(): Number {
    return (this / 30.48)
}

fun Double.footToCm(): Number {
    return (this * 30.48).roundToInt()
}

fun Number.roundUp(): Int {
    return try {
        DecimalFormat("#").apply {
            roundingMode = RoundingMode.UP
        }.format(this).toInt()
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
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
    val sizeInKb = (this / 1000f).toDouble()
    val sizeInMb = sizeInKb / 1000f
    val format = DecimalFormat("##.##")
    return when {
        sizeInMb >= 1 -> format.format(sizeInMb) + "MB"
        sizeInKb >= 1 -> format.format(sizeInKb) + "KB"
        else -> "${this}B"
    }
}

fun Long.convertMSIntoHourMinSeconds(): String {
    return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(this),
        TimeUnit.MILLISECONDS.toMinutes(this) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(this)),
        TimeUnit.MILLISECONDS.toSeconds(this) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this)))
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


fun Long.durationToMinSecShort(): String {
    val timeFormatter = SimpleDateFormat("m:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return timeFormatter.format(Date(this))
}

fun Float.inNotNanOrZero(): Float {
    return if (isNaN()) 0f else this
}