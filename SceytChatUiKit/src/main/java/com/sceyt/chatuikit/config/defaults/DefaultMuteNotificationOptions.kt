package com.sceyt.chatuikit.config.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.config.IntervalOption
import com.sceyt.chatuikit.config.MuteNotificationOptions
import kotlin.time.Duration.Companion.hours

data object DefaultMuteNotificationOptions : MuteNotificationOptions {
    override fun getOptions(context: Context) = listOf(
        IntervalOption(context.getString(R.string.sceyt_for_1_hour), 1.hours.inWholeMilliseconds),
        IntervalOption(context.getString(R.string.sceyt_for_8_hours), 8.hours.inWholeMilliseconds),
        IntervalOption(context.getString(R.string.sceyt_mute_forever), 0)
    )
}
