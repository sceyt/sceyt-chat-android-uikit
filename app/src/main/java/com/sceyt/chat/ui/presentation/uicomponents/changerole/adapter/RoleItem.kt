package com.sceyt.chat.ui.presentation.uicomponents.changerole.adapter

import com.sceyt.chat.models.role.Role

data class RoleItem(
        val role: Role
) {
    var checked: Boolean = false
}
