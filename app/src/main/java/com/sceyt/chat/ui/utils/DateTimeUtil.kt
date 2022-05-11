package com.sceyt.chat.ui.utils

import android.annotation.SuppressLint
import android.text.format.DateFormat
import androidx.annotation.IntRange
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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

    fun checkDateDiffMinuteWithCurrentUTC(date: String?): Int {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat(SERVER_DATE_PATTERN, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        return try {
            val date2: Calendar = Calendar.getInstance().apply {
                time = sdf.parse(date)!!
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
}