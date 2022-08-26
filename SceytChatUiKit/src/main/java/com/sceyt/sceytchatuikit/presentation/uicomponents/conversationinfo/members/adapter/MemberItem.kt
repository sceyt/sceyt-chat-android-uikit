package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter

import com.sceyt.sceytchatuikit.data.models.channels.SceytMember

sealed class MemberItem {
    data class Member(var member: SceytMember) : MemberItem()
    object LoadingMore : MemberItem()

    override fun equals(other: Any?): Boolean {
        return if (this is Member && other is Member) {
            member.id == other.member.id
        } else super.equals(other)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
