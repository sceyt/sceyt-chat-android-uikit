package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.extensions.getPeer

open class DefaultChannelNameFormatter : Formatter<SceytChannel> {

    override fun format(context: Context, from: SceytChannel): CharSequence {
        return when {
            from.isGroup -> from.subject.orEmpty()
            from.isSelf -> context.getString(R.string.sceyt_self_notes)
            else -> {
                val member = from.getPeer() ?: return ""
                SceytChatUIKit.formatters.userNameFormatter.format(context, member.user)
            }
        }
    }
}
