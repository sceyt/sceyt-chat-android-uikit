package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.formatters.Formatter

data object DefaultMentionUserNameFormatter : Formatter<User> {
    override fun format(context: Context, from: User): String {
        return if (from.activityState == UserState.Deleted)
            context.getString(R.string.sceyt_deleted_user)
        else from.getPresentableName()
    }
}

