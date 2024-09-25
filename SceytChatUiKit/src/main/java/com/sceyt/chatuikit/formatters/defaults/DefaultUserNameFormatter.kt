package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultUserNameFormatter : Formatter<User> {
    override fun format(context: Context, from: User): String {
        return from.getPresentableNameCheckDeleted(context)
    }
}

