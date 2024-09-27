package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

open class DefaultMediaDurationFormatter : Formatter<Long> {

    override fun format(context: Context, from: Long): CharSequence {
        return DateTimeUtil.secondsToTime(from)
    }
}