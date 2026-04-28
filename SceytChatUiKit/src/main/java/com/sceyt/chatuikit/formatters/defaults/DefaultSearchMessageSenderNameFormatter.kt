package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultSearchMessageSenderNameFormatter : Formatter<SceytMessage> {

    override fun format(context: Context, from: SceytMessage): CharSequence {
        return when {
            from.incoming -> {
                val name = from.user?.let {
                    SceytChatUIKit.formatters.userShortNameFormatter.format(context, it)
                }
                if (!name.isNullOrBlank()) "$name: " else ""
            }

            else -> "${context.getString(R.string.sceyt_your_last_message)}: "
        }
    }
}
