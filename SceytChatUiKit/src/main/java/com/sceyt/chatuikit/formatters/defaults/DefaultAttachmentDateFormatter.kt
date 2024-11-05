package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

open class DefaultAttachmentDateFormatter : Formatter<Date> {

    override fun format(context: Context, from: Date): String {
        return DateTimeUtil.getDateTimeString(from.time, "dd.MM.yy, HH:mm")
    }
}
