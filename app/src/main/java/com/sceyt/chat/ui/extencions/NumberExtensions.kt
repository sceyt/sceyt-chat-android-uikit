package com.sceyt.chat.ui.extencions

import java.math.RoundingMode
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