package com.sceyt.sceytchatuikit.extensions

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember

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
        user.id ?: ""
    }
}

fun SceytMember.getPresentableFirstName(): String {
    return user.firstName.ifBlank {
        user.id ?: ""
    }
}

fun User.getPresentableFirstName(): String {
    return firstName.ifBlank {
        id ?: ""
    }
}

val Any.TAG: String
    get() = javaClass.simpleName