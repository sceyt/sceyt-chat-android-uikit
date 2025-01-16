package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.push.PushData

open class DefaultNotificationBodyFormatter : Formatter<PushData> {

    override fun format(context: Context, from: PushData): CharSequence {
        return from.message.body
    }
}