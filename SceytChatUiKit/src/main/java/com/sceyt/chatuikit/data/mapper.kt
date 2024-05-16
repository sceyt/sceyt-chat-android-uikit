package com.sceyt.chatuikit.data

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem


fun Member.toSceytMember() = SceytMember(
    role = role,
    user = this
)

fun SceytMember.toMember(): Member {
    return Member(role, user)
}

fun SceytAttachment.toFileListItem(message: SceytMessage): FileListItem {
    return when (type) {
        AttachmentTypeEnum.Image.value() -> FileListItem.Image(this, message)
        AttachmentTypeEnum.Video.value() -> FileListItem.Video(this, message)
        AttachmentTypeEnum.Voice.value() -> FileListItem.Voice(this, message)
        else -> FileListItem.File(this, message)
    }
}

fun Presence.hasDiff(other: Presence): Boolean {
    return state != other.state || status != other.status || lastActiveAt != other.lastActiveAt
}


fun User.copy() = User(
    id, firstName, lastName, avatarURL, metadata, presence?.copy(), activityState, blocked
)

fun Presence.copy() = Presence(
    state, status, lastActiveAt
)