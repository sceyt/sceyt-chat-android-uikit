package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.formatters.Formatter

data object DefaultMentionUserNameFormatter : Formatter<SceytUser> {
    override fun format(context: Context, from: SceytUser): String {
        return if (from.state == UserState.Deleted)
            context.getString(R.string.sceyt_deleted_user)
        else from.getPresentableName()
    }
}

