package com.sceyt.chatuikit.data.models.channels

import com.sceyt.chatuikit.SceytChatUIKit

enum class RoleTypeEnum {
    None,
    Owner,
    Admin,
    Member;

    override fun toString(): String {
        return when (this) {
            None -> ""
            Owner -> SceytChatUIKit.config.memberRolesConfig.owner
            Admin -> SceytChatUIKit.config.memberRolesConfig.admin
            Member -> SceytChatUIKit.config.memberRolesConfig.participant
        }
    }
}
