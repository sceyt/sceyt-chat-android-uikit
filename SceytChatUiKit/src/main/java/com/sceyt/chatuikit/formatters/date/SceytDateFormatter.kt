package com.sceyt.chatuikit.formatters.date

import android.content.Context
import android.text.format.DateFormat
import com.sceyt.chatuikit.R
import java.util.Calendar

open class SceytDateFormatter {
    open fun today(context: Context) = DateFormatData(beginTittle = context.getString(R.string.sceyt_today))
    open fun thisWeek(context: Context): DateFormatData? = null
    open fun thisYear(context: Context) = DateFormatData(format = "MMMM dd")
    open fun olderThisYear(context: Context) = DateFormatData(format = "MMMM dd, yyyy")

    open fun getDateTimeStringWithDateFormatter(
            context: Context,
            time: Long?,
    ): String {
        if (time == null) return ""
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.timeInMillis = time

        val isThisYear = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
        val isToday = isThisYear && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        val formatter = if (isToday) {
            today(context)
        } else {
            val thisWeekFormatter = thisWeek(context)
            when {
                thisWeekFormatter != null && cal.isInSameWeekAs(now) -> thisWeekFormatter
                isThisYear -> thisYear(context)
                else -> olderThisYear(context)
            }
        }

        return if (formatter.shouldFormat)
            "${formatter.beginTittle}${DateFormat.format(formatter.format, cal)}${formatter.endTitle}"
        else formatter.beginTittle + formatter.endTitle
    }

    private fun Calendar.isInSameWeekAs(other: Calendar): Boolean {
        val startOfWeek = (other.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val daysSinceStartOfWeek =
                (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
            add(Calendar.DAY_OF_MONTH, -daysSinceStartOfWeek)
        }
        val startOfNextWeek = (startOfWeek.clone() as Calendar).apply {
            add(Calendar.WEEK_OF_YEAR, 1)
        }
        return !before(startOfWeek) && before(startOfNextWeek)
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
