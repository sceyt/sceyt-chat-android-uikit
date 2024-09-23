package com.sceyt.chatuikit.formatters.date

import android.content.Context

open class ConversationMediaDateFormatter : SceytDateFormatter() {
    override fun today(context: Context) = DateFormatData(format = "MMMM d")
    override fun thisYear(context: Context) = DateFormatData(format = "MMMM d")
}