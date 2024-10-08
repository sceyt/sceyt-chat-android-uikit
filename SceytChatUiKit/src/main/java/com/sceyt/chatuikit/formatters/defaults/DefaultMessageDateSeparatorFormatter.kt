package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.date.SceytDateFormatter
import java.util.Date

open class DefaultMessageDateSeparatorFormatter : Formatter<Date> {

    override fun format(context: Context, from: Date): String {
        return dateFormatter.getDateTimeStringWithDateFormatter(context, from.time)
    }

    protected open val dateFormatter = SceytDateFormatter()
}
