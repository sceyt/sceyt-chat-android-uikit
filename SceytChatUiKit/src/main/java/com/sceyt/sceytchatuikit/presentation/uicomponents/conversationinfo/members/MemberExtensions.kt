package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff

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

