package com.sceyt.sceytchatuikit.extensions

import android.content.Context
import androidx.fragment.app.Fragment
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
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
    return if (activityState == UserState.Deleted)
        context.getString(R.string.sceyt_deleted_user)
    else getPresentableName()
}

fun SceytMember.getPresentableNameCheckDeleted(context: Context): String {
    return if (user.activityState == UserState.Deleted)
        context.getString(R.string.sceyt_deleted_user)
    else getPresentableName()
}

private fun isDeletedUser(status: UserState): Boolean {
    return status == UserState.Deleted
}


val Any.TAG: String
    get() = this::class.java.simpleName

val Fragment.TAG_NAME: String
    get() = javaClass.simpleName

val Any.TAG_REF: String
    get() = this.toString()