package com.sceyt.chatuikit.shared.utils

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.formatters.BaseDateFormatter
import com.sceyt.chatuikit.config.formatters.DateFormatData
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object DateTimeUtil {
    const val SERVER_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
    const val SERVER_DATE_PATTERN_SSS = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    const val DAY_MONTH_YEAR_PATTERN = "d MMM yyyy"
    const val DAY_MONTH_YEAR_TIME_PATTERN = "d MMM yyyy, HH:mm"
    const val DAY_MONTH_YEAR_TIME_AM_PM_PATTERN = "d MMM yyyy, hh:mm a"
    const val DAY_MONTH_TIME_PATTERN = "d MMM HH:mm a"
    const val DAY_MONTH_PATTERN = "d MMM"
    const val TIME_PATTERN = "HH:mm"
    const val TIME_PATTERN_AM_PM = "hh:mm a"
    const val NOTIFICATION_TIME_PATTERN = "d MMM, HH:mm"

    fun convertServerDate(date: String?, convertingDatePattern: String): String {
        val sdf = SimpleDateFormat(SERVER_DATE_PATTERN, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val sdf1 = SimpleDateFormat(convertingDatePattern, Locale.ENGLISH)
        sdf1.timeZone = TimeZone.getDefault()

        return try {
            date?.let { sdf1.format(sdf.parse(it)!!) }.toString()
        } catch (e: ParseException) {
            e.printStackTrace()
            ""
        }
    }

    fun convertToServerUTCDate(date: Date, convertingDatePattern: String): Date {
        val sdf = SimpleDateFormat(convertingDatePattern, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val stringDate = sdf.format(date)
        val sdf2 = SimpleDateFormat(convertingDatePattern, Locale.ENGLISH)
        return try {
            sdf2.parse(stringDate) ?: return date
        } catch (e: ParseException) {
            e.printStackTrace()
            date
        }
    }

    fun convertToServerUTCDate(date: String?, convertingDatePattern: String): Date? {
        val sdf = SimpleDateFormat(convertingDatePattern, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val convertedDate = convertStringToDate(date, SERVER_DATE_PATTERN) ?: return null
        val stringDate = sdf.format(convertedDate)
        val sdf2 = SimpleDateFormat(convertingDatePattern, Locale.ENGLISH)
        return try {
            sdf2.parse(stringDate)
        } catch (e: ParseException) {
            e.printStackTrace()
            Date()
        }
    }

    fun checkDateDiffMinuteWithCurrentUTC(date: String): Int {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat(SERVER_DATE_PATTERN, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        return try {
            val date2: Calendar = Calendar.getInstance().apply {
                time = sdf.parse(date) ?: return -1
            }
            now.get(Calendar.MINUTE) - date2.get(Calendar.MINUTE)
        } catch (e: ParseException) {
            e.printStackTrace()
            -1
        }
    }

    fun convertDateToStringUTC(date: Date, datePattern: String): String {
        val sdf = SimpleDateFormat(datePattern, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    fun convertStringToDate(date: String?, datePattern: String): Date? {
        val sdf = SimpleDateFormat(datePattern, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            date?.let { sdf.parse(it) }
        } catch (e: ParseException) {
            e.printStackTrace()
            Date()
        }
    }

    fun convertDateToString(date: Date, datePattern: String): String {
        val sdf = SimpleDateFormat(datePattern, Locale.ENGLISH)
        return sdf.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun getDates(dateString1: String, dateString2: String): List<Date> {
        val dates = ArrayList<Date>()
        val df1 = SimpleDateFormat("yyyy-MM-dd")
        var date1: Date? = null
        var date2: Date? = null
        try {
            date1 = df1.parse(dateString1)
            date2 = df1.parse(dateString2)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        val cal1 = Calendar.getInstance()
        cal1.time = date1!!
        val cal2 = Calendar.getInstance()
        cal2.time = date2!!
        while (!cal1.after(cal2)) {
            dates.add(cal1.time)
            cal1.add(Calendar.DATE, 1)
        }
        return dates
    }

    fun convertMillisToString(millis: Long): String {
        return if (TimeUnit.MILLISECONDS.toHours(millis) == 0L)
            String.format(
                Locale.ENGLISH, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(millis)
                ),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(millis)
                )
            )
        else
            String.format(
                Locale.ENGLISH, "%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(millis)
                )
            )
    }

    fun getDateTimeStringWithDateFormatter(context: Context, time: Long?, dateFormatter: BaseDateFormatter): String {
        if (time == null) return ""
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.timeInMillis = time

        val isThisYear = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
        val formatter = when {
            isThisYear && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) -> {
                dateFormatter.today(context)
            }

            isThisYear -> dateFormatter.thisYear(context)
            else -> dateFormatter.olderThisYear(context)
        }

        return if (formatter.shouldFormat)
            "${formatter.beginTittle}${DateFormat.format(formatter.format, cal)}${formatter.endTitle}"
        else formatter.beginTittle + formatter.endTitle
    }

    fun getDateTimeString(time: Long?, format: String = "HH:mm"): String {
        if (time == null) return ""
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        return DateFormat.format(format, cal).toString()
    }

    fun isSameDay(epochOne: Long, epochTwo: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return (fmt.format(epochOne) == fmt.format(epochTwo))
    }

    fun setLastActiveDateByTime(durationMs: Long): String {
        val now = Calendar.getInstance()
        val lastActiveAt = Calendar.getInstance()
        lastActiveAt.timeInMillis -= (now.timeInMillis - durationMs * 1000)

        val sdf = SimpleDateFormat("dd, MMM/yyyy HH:mm", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        return try {
            return sdf.format(lastActiveAt.time)
        } catch (e: ParseException) {
            e.printStackTrace()
            ""
        }
    }

    fun getPresenceDateFormatData(context: Context, date: Date): String {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat(SERVER_DATE_PATTERN, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val sdf1 = SimpleDateFormat(TIME_PATTERN_AM_PM, Locale.getDefault())
        sdf1.timeZone = TimeZone.getDefault()

        return try {
            val date2: Calendar = Calendar.getInstance().apply {
                time = date
            }

            val yearsDiff = (now.get(Calendar.YEAR) - date2.get(Calendar.YEAR)).absoluteValue
            val weeksDiff = (now.get(Calendar.WEEK_OF_YEAR) - date2.get(Calendar.WEEK_OF_YEAR)).absoluteValue
            val daysDiff = (now.get(Calendar.DAY_OF_YEAR) - date2.get(Calendar.DAY_OF_YEAR)).absoluteValue
            val hoursDiff = TimeUnit.MILLISECONDS.toHours(now.timeInMillis - date2.timeInMillis).toInt().absoluteValue
            val minDiff = TimeUnit.MILLISECONDS.toMinutes(now.timeInMillis - date2.timeInMillis).toInt().absoluteValue

            val dateFormatter = SceytChatUIKit.formatters.userPresenceDateFormatter
            return when {
                yearsDiff > 0 -> {
                    getDateText(date, dateFormatter.olderThisYear(context, date))
                }

                weeksDiff > 0 -> {
                    getDateText(date, dateFormatter.thisYear(context, date))
                }

                weeksDiff == 0 && daysDiff > 1 -> {
                    getDateText(date, dateFormatter.currentWeek(context, date))
                }

                daysDiff == 1 -> {
                    getDateText(date, dateFormatter.yesterday(context, date))
                }

                hoursDiff > 1 -> {
                    getDateText(date, dateFormatter.today(context, date))
                }

                hoursDiff == 1 -> {
                    getDateText(date, dateFormatter.oneHourAgo(context, date))
                }

                minDiff > 1 -> {
                    getDateText(date, dateFormatter.lessThenOneHour(context, minDiff, date))
                }

                else -> {
                    getDateText(date, dateFormatter.oneMinAgo(context, date))
                }
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            ""
        }
    }

    private fun getDateText(createdAt: Date, data: DateFormatData): String {
        if (data.format == null)
            return "${data.beginTittle}${data.endTitle}".trim()

        val text = try {
            val simpleDateFormat = SimpleDateFormat(data.format, Locale.getDefault())
            "${data.beginTittle} ${simpleDateFormat.format(createdAt)} ${data.endTitle}"
        } catch (ex: Exception) {
            "${data.beginTittle} ${data.format} ${data.endTitle}"
        }
        return text.trim()
    }

    fun millisecondsToTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes =
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(hours)
        val seconds =
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(minutes)

        return if (hours > 0)
            if (hours > 9)
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            else
                String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        else
            if (minutes > 9)
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            else
                String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    fun secondsToTime(seconds: Long): String {
        return millisecondsToTime(seconds * 1000)
    }
}