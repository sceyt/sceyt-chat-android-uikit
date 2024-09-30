package com.sceyt.chatuikit.extensions

import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.Size
import android.util.TypedValue
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

fun Number.toPrettySize(format: String = "%.2f"): String {
    val sizeInKb = toDouble() / 1000
    val sizeInMb = sizeInKb / 1000f
    return when {
        sizeInMb >= 1 -> String.format(Locale.getDefault(), format, sizeInMb) + "MB"
        sizeInKb >= 1 -> String.format(Locale.getDefault(), format, sizeInKb) + "KB"
        else -> "${this}B"
    }
}

fun Long.convertMSIntoHourMinSeconds(): String {
    return String.format(Locale.getDefault(), "%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(this),
        TimeUnit.MILLISECONDS.toMinutes(this) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(this)),
        TimeUnit.MILLISECONDS.toSeconds(this) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this)))
}

/**
 * Transforms DP value integer to pixels, based on the screen density.
 */
fun Int.dpToPx(): Int = dpToPxPrecise().roundToInt()

/**
 * Uses the display metrics to transform the value of DP to pixels.
 */
fun Int.dpToPxPrecise(): Float = (this * displayMetrics().density)

fun Float.spToPx(): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, displayMetrics())
}

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

fun calculateScaleWidthHeight(defaultSize: Int, minSize: Int, imageWidth: Int, imageHeight: Int): Size {
    val coefficient = imageWidth.toDouble() / imageHeight.toDouble()
    var scaleWidth = defaultSize
    var scaleHeight = defaultSize

    if (coefficient.isNaN()) {
        return Size(scaleWidth, scaleHeight)
    } else {
        if (coefficient != 1.0) {
            if (imageWidth > imageHeight) {
                val h = (defaultSize / coefficient).toInt()
                scaleHeight = if (h >= minSize)
                    h
                else minSize
            } else {
                val futureW = (defaultSize * coefficient).toInt()
                val coefficientWidth = futureW.toDouble() / defaultSize.toDouble()
                var newDefaultSize = defaultSize

                // If the width of the image is less than 80% of the default size, then we can increase the default size by 20%
                if (coefficientWidth <= 0.8)
                    newDefaultSize = (defaultSize * 1.2).toInt()

                val w = (newDefaultSize * coefficient).toInt()

                scaleWidth = if (w >= minSize)
                    w
                else minSize

                scaleHeight = newDefaultSize
            }
        }
        return Size(scaleWidth, scaleHeight)
    }
}

fun Boolean.toInt() = if (this) 1 else 0

fun Int.toBoolean() = this == 1
