package com.sceyt.chatuikit.formatters

import android.content.Context

open class ChannelDateFormatter : BaseDateFormatter() {
    override fun today(context: Context) = DateFormatData(format = "HH:mm")
    override fun thisYear(context: Context) = DateFormatData(format = "dd MMM")
    override fun olderThisYear(context: Context) = DateFormatData(format = "dd.MM.yy")
}