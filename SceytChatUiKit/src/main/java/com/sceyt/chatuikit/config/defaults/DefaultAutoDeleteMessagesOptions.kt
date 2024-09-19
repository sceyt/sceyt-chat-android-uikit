package com.sceyt.chatuikit.config.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.config.IntervalOption
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

open class DefaultAutoDeleteMessagesOptions(context: Context) {
    open val options = listOf(
        IntervalOption(context.getString(R.string.sceyt_1_day), 1.hours.inWholeMilliseconds),
        IntervalOption(context.getString(R.string.sceyt_1_week), 7.days.inWholeMilliseconds),
        IntervalOption(context.getString(R.string.sceyt_1_month), 31.days.inWholeMilliseconds),
        IntervalOption(context.getString(R.string.sceyt_off), 0)
    )
}
