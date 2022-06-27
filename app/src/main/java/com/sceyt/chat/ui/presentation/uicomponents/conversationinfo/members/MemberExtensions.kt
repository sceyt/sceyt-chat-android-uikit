package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members

import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff

fun SceytMember.diff(other: SceytMember, showMoreChanged: Boolean) = MemberItemPayloadDiff(
    avatarChanged = avatarURL != other.avatarURL,
    nameChanged = fullName != other.firstName,
    onlineStateChanged = presence?.state != other.presence?.state,
    roleChanged = role.name != other.role.name,
    showMorItemChanged = showMoreChanged
)