package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.extensions.getPresentableFirstName
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultTypingUserNameFormatter : Formatter<User> {
    override fun format(context: Context, from: User): String {
        return from.getPresentableFirstName()
    }
}

