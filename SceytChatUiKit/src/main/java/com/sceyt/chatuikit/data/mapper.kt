package com.sceyt.chatuikit.data

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.Presence
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.mappers.toTransferData
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
    val type = when (type) {
        AttachmentTypeEnum.Image.value -> AttachmentTypeEnum.Image
        AttachmentTypeEnum.Video.value -> AttachmentTypeEnum.Video
        AttachmentTypeEnum.Voice.value -> AttachmentTypeEnum.Voice
        else -> AttachmentTypeEnum.File
    }

    return FileListItem(
        _attachment = this,
        _metadataPayload = getInfoFromMetadata(),
        type = type,
        _thumbPath = null,
        _transferData = toTransferData()
    )
}

fun Presence.hasDiff(other: Presence?): Boolean {
    other ?: return true
    return state != other.state || status != other.status || lastActiveAt != other.lastActiveAt
}