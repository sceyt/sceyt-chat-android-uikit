package com.sceyt.sceytchatuikit.extensions

import android.content.Context
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserActivityStatus
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember

fun Member.getPresentableName(): String {
    return (this as User).getPresentableName()
}

fun User.getPresentableName(): String {
    return fullName.ifBlank {
        id
    }.trim()
}

fun User.getPresentableFirstName(): String {
    return firstName.ifBlank {
        id ?: ""
    }.trim()
}

fun SceytMember.getPresentableName(): String {
    return user.getPresentableName()
}

fun SceytMember.getPresentableNameWithYou(context: Context): String {
    return user.getPresentableNameWithYou(context)
}

fun User.getPresentableNameWithYou(context: Context): String {
    if (SceytKitClient.myId == id)
        return context.getString(R.string.sceyt_you)
    return getPresentableNameCheckDeleted(context)
}

fun SceytMember.getPresentableFirstName(): String {
    return user.getPresentableFirstName()
}

fun User.getPresentableNameCheckDeleted(context: Context): String {
    return if (activityState == UserActivityStatus.Deleted)
        context.getString(R.string.sceyt_deleted_user)
    else getPresentableName()
}

fun SceytMember.getPresentableNameCheckDeleted(context: Context): String {
    return if (user.activityState == UserActivityStatus.Deleted)
        context.getString(R.string.sceyt_deleted_user)
    else getPresentableName()
}

private fun isDeletedUser(status: UserActivityStatus): Boolean {
    return status == UserActivityStatus.Deleted
}


val Any.TAG: String
    get() = javaClass.simpleName