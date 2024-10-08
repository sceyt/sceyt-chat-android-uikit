package com.sceyt.chatuikit.formatters.date

import android.content.Context
import com.sceyt.chatuikit.R
import java.text.SimpleDateFormat
import java.util.*

open class PresenceDateFormatter {
    open fun oneMinAgo(context: Context, date: Date) = DateFormatData(
        beginTittle = "${context.getString(R.string.sceyt_last_seen)} ${context.getString(R.string.sceyt_1_min_ago)}")

    open fun lessThenOneHour(context: Context, minutes: Int, date: Date) = DateFormatData(
        beginTittle = "${context.getString(R.string.sceyt_last_seen)} $minutes ${context.getString(R.string.sceyt_minutes_ago)}",
    )

    open fun oneHourAgo(context: Context, date: Date) = DateFormatData(
        beginTittle = context.getString(R.string.sceyt_last_seen),
        format = "HH:mm"
    )

    open fun today(context: Context, date: Date) = DateFormatData(
        beginTittle = context.getString(R.string.sceyt_last_seen),
        format = "HH:mm"
    )

    open fun yesterday(context: Context, date: Date) = DateFormatData(
        beginTittle = "${context.getString(R.string.sceyt_last_seen)} ${context.getString(R.string.sceyt_yesterday_at)}",
        format = "HH:mm"
    )

    open fun currentWeek(context: Context, date: Date) = DateFormatData(
        beginTittle = "${context.getString(R.string.sceyt_last_seen)} ${SimpleDateFormat("EEEE", Locale.getDefault()).format(date)}" +
                " ${context.getString(R.string.sceyt_at)}", format = "HH:mm")

    open fun thisYear(context: Context, date: Date) = DateFormatData(
        beginTittle = context.getString(R.string.sceyt_last_seen),
        format = "dd.MM.yy")

    open fun olderThisYear(context: Context, date: Date) = DateFormatData(
        beginTittle = context.getString(R.string.sceyt_last_seen),
        format = "dd.MM.yy")
}
