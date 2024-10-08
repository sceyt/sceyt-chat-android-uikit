package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.formatters.Formatter
import java.text.NumberFormat
import java.util.Locale

data object DefaultUnreadCountFormatter : Formatter<Long> {

    override fun format(context: Context, from: Long): CharSequence {
        // User NumberFormat for arabic language
        return if (from > 99L) {
            "${NumberFormat.getInstance(Locale.getDefault()).format(99)}+"
        } else NumberFormat.getInstance(Locale.getDefault()).format(from)
    }
}