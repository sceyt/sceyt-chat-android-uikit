package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultPollVoteCountFormatter : Formatter<PollOptionUiModel> {
    override fun format(context: Context, from: PollOptionUiModel): String {
        return if (from.voteCount > 99)
            "99+" else from.voteCount.toString()
    }
}

