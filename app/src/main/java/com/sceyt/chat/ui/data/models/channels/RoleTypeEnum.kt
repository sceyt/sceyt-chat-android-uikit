package com.sceyt.chat.ui.data.models.channels

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
