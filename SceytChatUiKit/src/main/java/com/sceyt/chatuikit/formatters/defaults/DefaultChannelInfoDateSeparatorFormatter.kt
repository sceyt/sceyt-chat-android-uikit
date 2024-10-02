package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.date.DateFormatData
import com.sceyt.chatuikit.formatters.date.SceytDateFormatter
import java.util.Date

open class DefaultChannelInfoDateSeparatorFormatter : Formatter<Date> {

    override fun format(context: Context, from: Date): String {
        return dateFormatter.getDateTimeStringWithDateFormatter(context, from.time)
    }

    protected open val dateFormatter = object : SceytDateFormatter() {
        override fun today(context: Context) = DateFormatData(format = "MMMM d")
        override fun thisYear(context: Context) = DateFormatData(format = "MMMM d")
    }
}
