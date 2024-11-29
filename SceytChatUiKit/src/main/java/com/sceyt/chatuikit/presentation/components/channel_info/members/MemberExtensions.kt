package com.sceyt.chatuikit.presentation.components.channel_info.members

import com.sceyt.chatuikit.data.hasDiff
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.diff.MemberItemPayloadDiff

fun SceytMember.diff(other: SceytMember, showMoreChanged: Boolean) = MemberItemPayloadDiff(
    avatarChanged = user.avatarURL != other.user.avatarURL,
    nameChanged = fullName != other.user.firstName,
    presenceStateChanged = user.presence?.hasDiff(other.user.presence) == true,
    roleChanged = role.name != other.role.name,
    showMorIconChanged = showMoreChanged
)

