package com.sceyt.chatuikit.presentation.components.channel_info.members

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.diff.MemberItemPayloadDiff

fun SceytMember.diff(other: SceytMember, showMoreChanged: Boolean) = MemberItemPayloadDiff(
    avatarChanged = user.avatarURL != other.user.avatarURL,
    nameChanged = fullName != other.user.firstName,
    onlineStateChanged = user.presence?.state != other.user.presence?.state,
    roleChanged = role.name != other.role.name,
    showMorIconChanged = showMoreChanged
)

fun genMemberBy(user: User): Member {
    return Member(Role(""), user)
}

