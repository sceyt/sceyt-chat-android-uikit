package com.sceyt.sceytchatuikit.sceytconfigs.dateformaters

import android.content.Context

open class ConversationMediaDateFormatter : BaseDateFormatter() {
    override fun today(context: Context) = DateFormatData(format = "MMMM d")
    override fun thisYear(context: Context) = DateFormatData(format = "MMMM d")
    override fun olderThisYear(context: Context) = DateFormatData(format = "dd.MM.yy")
}