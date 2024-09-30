package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter

data object DefaultMessageViewCountFormatter : Formatter<Long> {
    override fun format(context: Context, from: Long): CharSequence {
        return when {
            from < 1000 -> from.toString()
            from < 1000000 -> "${from / 1000}K"
            else -> "${from / 1000000}M"
        }
    }
}