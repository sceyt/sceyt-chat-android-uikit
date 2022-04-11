package com.sceyt.chat.ui.extencions

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import java.text.DecimalFormat

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
