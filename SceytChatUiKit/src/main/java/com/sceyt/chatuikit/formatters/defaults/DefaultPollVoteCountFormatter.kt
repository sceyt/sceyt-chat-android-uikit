package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultPollVoteCountFormatter : Formatter<PollOption> {
    override fun format(context: Context, from: PollOption): String {
        return from.voteCount.toString()
    }
}

