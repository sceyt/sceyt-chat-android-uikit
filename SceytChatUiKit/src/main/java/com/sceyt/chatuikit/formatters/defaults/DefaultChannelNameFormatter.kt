package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isSelf

data object DefaultChannelNameFormatter : Formatter<SceytChannel> {

    override fun format(context: Context, from: SceytChannel): CharSequence {
        return when {
            from.isGroup -> from.channelSubject
            from.isSelf() -> context.getString(R.string.sceyt_self_notes)
            else -> {
                val member = from.getPeer() ?: return ""
                SceytChatUIKit.formatters.userNameFormatterNew.format(context, member.user)
            }
        }
    }
}
