package com.sceyt.chatuikit.formatters

import android.content.Context
import com.sceyt.chatuikit.formatters.date.BaseDateFormatter
import java.util.Date

interface MessageDateSeparatorFormatter : Formatter<Date>

open class DefaultMessageDateSeparatorFormatter : MessageDateSeparatorFormatter {

    override fun format(context: Context, from: Date): String {
        return dateFormatter.getDateTimeStringWithDateFormatter(context, from.time)
    }

    protected open val dateFormatter = BaseDateFormatter()
}
