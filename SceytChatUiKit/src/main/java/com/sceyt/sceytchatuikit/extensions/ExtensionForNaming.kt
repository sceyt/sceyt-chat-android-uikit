package com.sceyt.sceytchatuikit.extensions

import android.content.Context
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember

fun Member.getPresentableName(): String {
    return fullName.ifBlank {
        id
    }.trim()
}

fun User.getPresentableName(): String {
    return fullName.ifBlank {
        id
    }.trim()
}

fun SceytMember.getPresentableName(): String {
    return fullName.ifBlank {
        user.id ?: ""
    }.trim()
}

fun SceytMember.getPresentableNameWithYou(context: Context, mId: String?): String {
    if (mId == id)
        return context.getString(R.string.sceyt_you)
    return fullName.ifBlank {
        user.id ?: ""
    }.trim()
}

fun SceytMember.getPresentableFirstName(): String {
    return user.firstName.ifBlank {
        user.id ?: ""
    }.trim()
}

fun User.getPresentableFirstName(): String {
    return firstName.ifBlank {
        id ?: ""
    }.trim()
}

val Any.TAG: String
    get() = javaClass.simpleName