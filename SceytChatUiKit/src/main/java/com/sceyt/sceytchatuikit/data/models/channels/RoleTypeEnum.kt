package com.sceyt.sceytchatuikit.data.models.channels

enum class RoleTypeEnum {
    None,
    Owner,
    Admin,
    Member;

    override fun toString(): String {
        return when (this) {
            None -> ""
            Owner -> "owner"
            Admin -> "admin"
            Member -> "participant"
        }
    }
}
