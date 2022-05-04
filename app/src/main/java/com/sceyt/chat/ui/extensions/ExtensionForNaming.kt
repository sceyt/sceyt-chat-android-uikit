package com.sceyt.chat.ui.extensions

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User

fun Member.getPresentableName(): String {
    return fullName.ifBlank {
        id
    }
}

fun User.getPresentableName(): String {
    return fullName.ifBlank {
        id
    }
}
