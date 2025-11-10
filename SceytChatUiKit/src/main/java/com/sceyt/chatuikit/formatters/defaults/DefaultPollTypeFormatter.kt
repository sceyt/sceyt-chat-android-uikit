package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultPollTypeFormatter : Formatter<SceytPollDetails> {
    override fun format(context: Context, from: SceytPollDetails): String {
        return from.run {
            when {
                closed -> context.getString(R.string.sceyt_poll_finished)
                anonymous -> {
                    String.format(
                        context.getString(R.string.sceyt_anonymous_poll_and_type),
                        context.getTypeText(from)
                    )
                }

                else -> context.getTypeText(from)
            }
        }
    }

    open fun Context.getTypeText(poll: SceytPollDetails): String {
        return if (poll.allowMultipleVotes) {
            getString(R.string.multiple_votes)
        } else {
            getString(R.string.single_vote)
        }
    }
}

