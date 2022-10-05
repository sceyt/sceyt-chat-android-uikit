package com.sceyt.sceytchatuikit.sceytconfigs.dateformaters

import android.content.Context
import com.sceyt.sceytchatuikit.R

open class MessageDateSeparatorFormatter {
    open fun today(context: Context) = DateFormatData(beginTittle = context.getString(R.string.sceyt_today))
    open fun thisYear(context: Context) = DateFormatData(format = "MMMM dd")
    open fun olderThisYear(context: Context) = DateFormatData(format = "dd.MM.yy")
}

data class DateFormatData(
        val format: String? = null,
        val beginTittle: String = "",
        val endTitle: String = ""
)

