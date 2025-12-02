package com.sceyt.chatuikit.extensions

import android.content.Context
import com.sceyt.chatuikit.R
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

fun Long?.formatDisappearingMessagesDuration(context: Context): String {
    return when {
        this == null || this < 0 -> {
            context.resources.getQuantityString(R.plurals.sceyt_days, 0, 0)
        }

        this >= 2592000 -> {
            val months = this / 2592000
            context.resources.getQuantityString(R.plurals.sceyt_months, months.toInt(), months.toInt())
        }

        this >= 604800 -> {
            val weeks = this / 604800
            context.resources.getQuantityString(R.plurals.sceyt_weeks, weeks.toInt(), weeks.toInt())
        }

        else -> {
            val days = this / 86400
            context.resources.getQuantityString(R.plurals.sceyt_days, days.toInt(), days.toInt())
        }
    }
}