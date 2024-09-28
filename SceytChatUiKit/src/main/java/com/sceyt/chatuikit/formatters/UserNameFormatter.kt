package com.sceyt.chatuikit.formatters

import com.sceyt.chatuikit.data.models.messages.SceytUser

fun interface UserNameFormatter {
    fun format(user: SceytUser): String
}