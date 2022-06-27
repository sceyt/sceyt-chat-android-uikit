package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter

import com.sceyt.chat.ui.data.models.channels.SceytMember

sealed class MemberItem {
    data class Member(val member: SceytMember) : MemberItem()
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
