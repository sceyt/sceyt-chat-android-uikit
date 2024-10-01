package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.shared.utils.DateTimeUtil.getDateTimeString
import java.util.Date

data object DefaultMessageInfoDateFormatter : Formatter<Date> {

    override fun format(context: Context, from: Date): String {
        return getDateTimeString(from.time, "dd.MM.yy")
    }
}
