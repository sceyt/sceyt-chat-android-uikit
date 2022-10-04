package com.sceyt.sceytchatuikit.sceytconfigs.dateformaters

import android.content.Context
import com.sceyt.sceytchatuikit.R

open class MessageDateSeparatorFormatter {
    open fun today(context: Context): String = context.getString(R.string.sceyt_today)
    open fun thisYear(context: Context): String = "MMMM dd"
    open fun olderThisYear(context: Context): String = "dd.MM.yy"
}