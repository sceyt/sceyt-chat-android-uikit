package com.sceyt.chatuikit.config.formatters

import android.content.Context

open class ConversationMediaDateFormatter : BaseDateFormatter() {
    override fun today(context: Context) = DateFormatData(format = "MMMM d")
    override fun thisYear(context: Context) = DateFormatData(format = "MMMM d")
}