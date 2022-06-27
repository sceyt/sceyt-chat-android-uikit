package com.sceyt.chat.ui.extensions

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.models.channels.SceytMember

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

fun SceytMember.getPresentableName(): String {
    return fullName.ifBlank {
        id
    }
}

val Any.TAG: String
    get() = javaClass.simpleName