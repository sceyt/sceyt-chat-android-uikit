package com.sceyt.chatuikit.extensions

import android.content.Context
import androidx.fragment.app.Fragment
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser

fun SceytUser.getPresentableName(): String {
    return fullName.ifBlank {
        id
    }.trim()
}

fun SceytUser.getPresentableFirstName(): String {
    return firstName.ifBlank {
        id
    }.trim()
}

fun SceytMember.getPresentableName(): String {
    return user.getPresentableName()
}

fun SceytMember.getPresentableNameWithYou(context: Context): String {
    return user.getPresentableNameWithYou(context)
}

fun SceytUser.getPresentableNameWithYou(context: Context): String {
    if (SceytChatUIKit.chatUIFacade.myId == id)
        return context.getString(R.string.sceyt_you)
    return getPresentableNameCheckDeleted(context)
}

fun SceytMember.getPresentableFirstName(): String {
    return user.getPresentableFirstName()
}

fun SceytUser.getPresentableNameCheckDeleted(context: Context): String {
    return if (state == UserState.Deleted)
        context.getString(R.string.sceyt_deleted_user)
    else getPresentableName()
}

fun SceytMember.getPresentableNameCheckDeleted(context: Context): String {
    return if (user.state == UserState.Deleted)
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