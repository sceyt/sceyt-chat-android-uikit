package com.sceyt.sceytchatuikit.data.models.channels

enum class RoleTypeEnum {
    None,
    Owner,
    Member;

    override fun toString(): String {
        return when (this) {
            None -> ""
            Owner -> "owner"
            Member -> "participant"
        }
    }
}
