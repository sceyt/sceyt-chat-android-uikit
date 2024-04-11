package com.sceyt.chatuikit.extensions

import java.util.*

fun Date.isThisYear(): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = time
    return Calendar.getInstance().get(Calendar.YEAR) - cal.get(Calendar.YEAR) == 0
}

fun Long.isThisYear(): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this
    return Calendar.getInstance().get(Calendar.YEAR) - cal.get(Calendar.YEAR) == 0
}