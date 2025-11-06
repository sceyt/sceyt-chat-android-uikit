package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultUserAndNotesNameFormatter : Formatter<SceytUser> {
    override fun format(context: Context, from: SceytUser): CharSequence {
        if (from.id == SceytChatUIKit.currentUserId) {
            return context.getString(R.string.sceyt_self_notes)
        }
        return SceytChatUIKit.formatters.userNameFormatter.format(context, from)
    }
}

