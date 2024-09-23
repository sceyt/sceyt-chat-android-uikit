package com.sceyt.chatuikit.formatters.date

import android.content.Context
import android.text.format.DateFormat
import com.sceyt.chatuikit.R
import java.util.Calendar

open class SceytDateFormatter {
    open fun today(context: Context) = DateFormatData(beginTittle = context.getString(R.string.sceyt_today))
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
        val formatter = when {
            isThisYear && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) -> {
                today(context)
            }

            isThisYear -> thisYear(context)
            else -> olderThisYear(context)
        }

        return if (formatter.shouldFormat)
            "${formatter.beginTittle}${DateFormat.format(formatter.format, cal)}${formatter.endTitle}"
        else formatter.beginTittle + formatter.endTitle
    }
}