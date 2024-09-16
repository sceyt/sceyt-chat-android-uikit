package com.sceyt.chatuikit.config.formatters

import com.sceyt.chat.models.user.User

fun interface UserNameFormatter {
    fun format(user: User): String
}