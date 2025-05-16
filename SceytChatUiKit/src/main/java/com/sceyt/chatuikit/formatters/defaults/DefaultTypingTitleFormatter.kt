package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.TypingTitleFormatterAttributes

open class DefaultTypingTitleFormatter : Formatter<TypingTitleFormatterAttributes> {

    override fun format(context: Context, from: TypingTitleFormatterAttributes): String {
        if (from.users.isEmpty()) return ""
        return if (from.channel.isGroup) {
            buildString {
                from.users.forEachIndexed { index, user ->
                    append(getFormattedName(context, user))
                    if (index < from.users.lastIndex)
                        append(", ")
                }

                append(" ${context.getString(R.string.sceyt_typing)}")
            }
        } else context.getString(R.string.sceyt_typing)
    }

    private fun getFormattedName(context: Context, user: SceytUser): CharSequence {
        return SceytChatUIKit.formatters.typingUserNameFormatter.format(context, user).take(10)
    }
}

