package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members

import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff

fun SceytMember.diff(other: SceytMember, showMoreChanged: Boolean) = MemberItemPayloadDiff(
    avatarChanged = user.avatarURL != other.user.avatarURL,
    nameChanged = fullName != other.user.firstName,
    onlineStateChanged = user.presence?.state != other.user.presence?.state,
    roleChanged = role.name != other.role.name,
    showMorItemChanged = showMoreChanged
)