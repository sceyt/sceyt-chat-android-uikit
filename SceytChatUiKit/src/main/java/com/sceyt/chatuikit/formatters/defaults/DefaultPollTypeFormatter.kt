package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytPoll
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultPollTypeFormatter : Formatter<SceytPoll> {
    override fun format(context: Context, from: SceytPoll): String {
        return from.run {
            when {
                closed -> context.getString(R.string.sceyt_poll_finished)
                anonymous -> context.getString(R.string.sceyt_anonymous_poll)
                else -> context.getString(R.string.sceyt_public_poll)
            }
        }
    }
}

