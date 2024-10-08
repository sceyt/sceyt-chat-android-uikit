package com.sceyt.chatuikit.data

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.Presence
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.mappers.toUser
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem

fun Member.toSceytMember() = SceytMember(
    role = role,
    user = toSceytUser()
)

fun SceytMember.toMember(): Member {
    return Member(role, user.toUser())
}

fun SceytAttachment.toFileListItem(): FileListItem {
    return when (type) {
        AttachmentTypeEnum.Image.value -> FileListItem.Image(this)
        AttachmentTypeEnum.Video.value -> FileListItem.Video(this)
        AttachmentTypeEnum.Voice.value -> FileListItem.Voice(this)
        else -> FileListItem.File(this)
    }
}

fun Presence.hasDiff(other: Presence): Boolean {
    return state != other.state || status != other.status || lastActiveAt != other.lastActiveAt
}