package com.sceyt.chatuikit.presentation.components.channel_info.members.adapter

import com.sceyt.chatuikit.data.models.channels.SceytMember

sealed class MemberItem {
    data class Member(var member: SceytMember) : MemberItem() {
        override fun equals(other: Any?): Boolean {
            return other is Member && member.id == other.member.id
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    object LoadingMore : MemberItem()
}
