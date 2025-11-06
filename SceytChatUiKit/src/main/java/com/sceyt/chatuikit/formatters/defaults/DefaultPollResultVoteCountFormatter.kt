package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultPollResultVoteCountFormatter : Formatter<Int> {
    override fun format(context: Context, from: Int): String {
        return context.resources.getQuantityString(
            R.plurals.sceyt_votes_count,
            from,
            from
        )
    }
}