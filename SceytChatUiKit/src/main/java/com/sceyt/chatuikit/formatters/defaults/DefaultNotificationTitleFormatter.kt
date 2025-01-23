package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.push.PushData

open class DefaultNotificationTitleFormatter : Formatter<PushData> {

    override fun format(context: Context, from: PushData): CharSequence {
        return if (from.channel.isGroup)
            from.channel.subject.orEmpty()
        else SceytChatUIKit.formatters.userNameFormatter.format(context, from.user)
    }
}