package com.sceyt.chatuikit.sceytconfigs

import com.sceyt.chat.models.user.User

fun interface UserNameFormatter {
    fun format(user: User): String
}