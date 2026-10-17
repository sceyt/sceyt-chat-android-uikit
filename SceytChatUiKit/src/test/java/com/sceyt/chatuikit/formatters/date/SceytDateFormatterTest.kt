package com.sceyt.chatuikit.formatters.date

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class SceytDateFormatterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `default formatters retain their original values and disable week formatting`() {
        val formatter = SceytDateFormatter()
        val resourceContext = mock<Context>()
        whenever(resourceContext.getString(R.string.sceyt_today)).thenReturn(TODAY)

        assertThat(formatter.today(resourceContext).beginTittle).isEqualTo(TODAY)
        assertThat(formatter.today(resourceContext).shouldFormat).isFalse()
        assertThat(formatter.thisWeek(context)).isNull()
        assertThat(formatter.thisYear(context).format).isEqualTo("MMMM dd")
        assertThat(formatter.olderThisYear(context).format).isEqualTo("MMMM dd, yyyy")
    }

    @Test
    fun `null time returns an empty string`() {
        val formatter = MarkerFormatter(weekEnabled = true)

        assertThat(formatter.getDateTimeStringWithDateFormatter(context, null)).isEmpty()
    }

    @Test
    fun `today uses the today formatter`() {
        val formatter = MarkerFormatter(weekEnabled = true)

        assertThat(formatter.format(Date())).isEqualTo(TODAY)
    }

    @Test
    fun `formatted data includes its beginning and ending titles`() {
        val formatter = object : MarkerFormatter(weekEnabled = false) {
            override fun today(context: Context) = DateFormatData(
                format = "'formatted'",
                beginTittle = "<",
                endTitle = ">"
            )
        }

        assertThat(formatter.format(Date())).isEqualTo("<formatted>")
    }

    @Test
    fun `current week uses the week formatter when enabled`() {
        val formatter = MarkerFormatter(weekEnabled = true)

        assertThat(formatter.format(anotherDayInCurrentWeek().time)).isEqualTo(THIS_WEEK)
    }

    @Test
    fun `current week uses the year formatter when week formatting is disabled`() {
        val formatter = MarkerFormatter(weekEnabled = false)

        assertThat(formatter.format(anotherDayInCurrentWeek().time)).isEqualTo(THIS_YEAR)
    }

    @Test
    fun `date outside the current week uses the year formatter`() {
        val formatter = MarkerFormatter(weekEnabled = true)

        assertThat(formatter.format(dateOutsideCurrentWeekInCurrentYear().time))
            .isEqualTo(THIS_YEAR)
    }

    @Test
    fun `date after the current week does not use the week formatter`() {
        val formatter = MarkerFormatter(weekEnabled = true)
        val now = Calendar.getInstance()
        val dateAfterCurrentWeek = startOfWeek(now).apply {
            add(Calendar.WEEK_OF_YEAR, 1)
        }
        val expected = if (dateAfterCurrentWeek.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            THIS_YEAR
        } else {
            OLDER
        }

        assertThat(formatter.format(dateAfterCurrentWeek.time)).isEqualTo(expected)
    }

    @Test
    fun `date from an older year uses the older formatter`() {
        val formatter = MarkerFormatter(weekEnabled = true)
        val olderDate = calendarDate(
            year = Calendar.getInstance().get(Calendar.YEAR) - 1,
            month = Calendar.JULY,
            dayOfMonth = 16
        )

        assertThat(formatter.format(olderDate.time)).isEqualTo(OLDER)
    }

    private fun anotherDayInCurrentWeek(): Calendar {
        val now = Calendar.getInstance()
        val candidate = startOfWeek(now)
        repeat(DAYS_IN_WEEK) {
            if (candidate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                !candidate.isSameDayAs(now)
            ) {
                return candidate
            }
            candidate.add(Calendar.DAY_OF_MONTH, 1)
        }
        error("The current week must contain another day in the current year")
    }

    private fun dateOutsideCurrentWeekInCurrentYear(): Calendar {
        val now = Calendar.getInstance()
        return listOf(Calendar.JANUARY, Calendar.JULY)
            .map { month -> calendarDate(now.get(Calendar.YEAR), month, 15) }
            .first { candidate -> !candidate.isInSameWeekAs(now) }
    }

    private fun calendarDate(year: Int, month: Int, dayOfMonth: Int) =
        Calendar.getInstance().apply {
            clear()
            set(year, month, dayOfMonth, 12, 0, 0)
        }

    private fun startOfWeek(calendar: Calendar) = (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val daysSinceStartOfWeek =
            (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
        add(Calendar.DAY_OF_MONTH, -daysSinceStartOfWeek)
    }

    private fun Calendar.isInSameWeekAs(other: Calendar): Boolean {
        val startOfWeek = startOfWeek(other)
        val startOfNextWeek = (startOfWeek.clone() as Calendar).apply {
            add(Calendar.WEEK_OF_YEAR, 1)
        }
        return !before(startOfWeek) && before(startOfNextWeek)
    }

    private fun Calendar.isSameDayAs(other: Calendar): Boolean {
        return get(Calendar.ERA) == other.get(Calendar.ERA) &&
                get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
                get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }

    private open inner class MarkerFormatter(
        private val weekEnabled: Boolean,
    ) : SceytDateFormatter() {
        override fun today(context: Context) = DateFormatData(beginTittle = TODAY)

        override fun thisWeek(context: Context): DateFormatData? {
            return if (weekEnabled) DateFormatData(beginTittle = THIS_WEEK) else null
        }

        override fun thisYear(context: Context) = DateFormatData(beginTittle = THIS_YEAR)

        override fun olderThisYear(context: Context) = DateFormatData(beginTittle = OLDER)

        fun format(date: Date): String {
            return getDateTimeStringWithDateFormatter(context, date.time)
        }
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val TODAY = "today"
        const val THIS_WEEK = "this week"
        const val THIS_YEAR = "this year"
        const val OLDER = "older"
    }
}
