package com.sceyt.chatuikit.data.models.channels

import com.sceyt.chatuikit.SceytChatUIKit

data class CreateChannelData(
        val type: String,
        val uri: String = "",
        val subject: String = "",
        val avatar: ChannelAvatar? = null,
        val metadata: String = "",
        val members: List<SceytMember> = emptyList(),
) {
    @JvmOverloads
    constructor(
            type: String,
            userIds: List<String>,
            roleName: String = SceytChatUIKit.config.memberRolesConfig.participant,
            uri: String = "",
            subject: String = "",
            avatar: ChannelAvatar? = null,
            metadata: String = "",
    ) : this(
        type = type,
        uri = uri,
        subject = subject,
        avatar = avatar,
        metadata = metadata,
        members = userIds.map { SceytMember(it, roleName) }
    )
}
