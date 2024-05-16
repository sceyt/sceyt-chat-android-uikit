package com.sceyt.chatuikit.sceytconfigs.dateformaters

import android.content.Context
import com.sceyt.chatuikit.R

open class BaseDateFormatter {
    open fun today(context: Context) = DateFormatData(beginTittle = context.getString(R.string.sceyt_today))
    open fun thisYear(context: Context) = DateFormatData(format = "MMMM dd")
    open fun olderThisYear(context: Context) = DateFormatData(format = "MMMM dd, yyyy")
}