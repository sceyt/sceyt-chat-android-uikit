package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isSelf

open class DefaultChannelNameFormatter : Formatter<SceytChannel> {

    override fun format(context: Context, from: SceytChannel): CharSequence {
        return if (from.isGroup) {
            from.channelSubject
        } else {
            if (from.isSelf()) {
                context.getString(R.string.sceyt_self_notes)
            } else {
                from.getPeer()?.user?.let { user ->
                    SceytChatUIKit.formatters.userNameFormatterNew.format(context, user)
                } ?: ""
            }
        }
    }
}
