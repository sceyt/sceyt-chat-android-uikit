package com.sceyt.sceytchatuikit.sceytconfigs.dateformaters

import android.content.Context

open class ChannelDateFormatter {
    open fun today(context: Context): String = "HH:mm"
    open fun thisYear(context: Context): String = "dd MMM"
    open fun olderThisYear(context: Context): String = "dd.MM.yy"
}