package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.UserActivityTitleFormatterAttributes
import com.sceyt.chatuikit.presentation.components.channel.input.data.UserActivity

open class DefaultChannelListUserActivityTitleFormatter : Formatter<UserActivityTitleFormatterAttributes> {

    override fun format(context: Context, from: UserActivityTitleFormatterAttributes): String {
        if (from.activeUsers.isEmpty()) return ""
        val text = if (from.activeUsers.any { it.activity == UserActivity.Typing })
            context.getString(R.string.sceyt_typing_)
        else context.getString(R.string.recording_)
        return if (from.channel.isGroup) {
            buildString {
                from.activeUsers.forEachIndexed { index, activeUser ->
                    append(getFormattedName(context, activeUser.user))
                    if (index < from.activeUsers.lastIndex)
                        append(", ")
                }

                append(" $text")
            }
        } else text
    }

    private fun getFormattedName(context: Context, user: SceytUser): CharSequence {
        return SceytChatUIKit.formatters.activityUserNameFormatter.format(context, user).take(10)
    }
}

