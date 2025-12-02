package com.sceyt.chatuikit.extensions

import android.content.Context
import com.sceyt.chatuikit.R
import java.util.Calendar
import java.util.Date
import kotlin.time.Duration.Companion.days

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
        this == null || this <= 0 -> {
            context.resources.getString(R.string.sceyt_unknown_duration)
        }

        this >=30.days.inWholeMilliseconds -> {
            context.resources.getString(R.string.sceyt_1_month)

        }

        this >= 7.days.inWholeMilliseconds -> {
            context.resources.getString(R.string.sceyt_1_week)
        }

        else -> {
            context.resources.getString(R.string.sceyt_1_day)
        }
    }
}