package com.sceyt.chatuikit.data.models.channels

import com.sceyt.chatuikit.SceytChatUIKit

enum class RoleTypeEnum {
    None,
    Owner,
    Admin,
    Member;

    val value: String
        get() = when (this) {
            None -> ""
            Owner -> SceytChatUIKit.config.memberRolesConfig.owner
            Admin -> SceytChatUIKit.config.memberRolesConfig.admin
            Member -> SceytChatUIKit.config.memberRolesConfig.participant
        }
}
