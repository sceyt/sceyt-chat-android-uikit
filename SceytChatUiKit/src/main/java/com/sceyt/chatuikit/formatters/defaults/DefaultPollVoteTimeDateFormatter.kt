package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.shared.utils.DateTimeUtil.getDateTimeString

open class DefaultPollVoteTimeDateFormatter : Formatter<Long> {

    override fun format(context: Context, from: Long): String {
        return getDateTimeString(from, "yy.MM.dd HH:mm")
    }
}