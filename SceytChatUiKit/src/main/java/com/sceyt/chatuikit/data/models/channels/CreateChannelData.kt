package com.sceyt.chatuikit.data.models.channels

import com.sceyt.chatuikit.SceytChatUIKit

data class CreateChannelData(
        var type: String,
        var uri: String = "",
        var subject: String = "",
        var avatarUrl: String = "",
        var metadata: String = "",
        var members: List<SceytMember> = emptyList(),
) {
    var avatarUploaded: Boolean = false

    @JvmOverloads
    constructor(
            type: String,
            userIds: List<String>,
            roleName: String = SceytChatUIKit.config.memberRolesConfig.participant,
            uri: String = "",
            subject: String = "",
            avatarUrl: String = "",
            metadata: String = "",
    ) : this(
        type = type,
        uri = uri,
        subject = subject,
        avatarUrl = avatarUrl,
        metadata = metadata,
        members = userIds.map { SceytMember(it, roleName) }
    )
}
