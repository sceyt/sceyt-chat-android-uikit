package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

data class MentionUserData(val member: SceytMember) {

    val id get() = member.id

    val user get() = member.user

    fun getMentionedName(): String {
        return "@${SceytKitConfig.userNameBuilder?.invoke(member.user) ?: member.getPresentableName()}"
    }

    override fun toString() = getMentionedName()
}