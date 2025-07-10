package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultChannelEventUserNameFormatter : Formatter<SceytUser> {
    override fun format(context: Context, from: SceytUser): CharSequence {
        return SceytChatUIKit.formatters.userShortNameFormatter.format(context, from)
    }
}