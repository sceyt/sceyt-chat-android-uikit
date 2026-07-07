package com.sceyt.chatuikit.config

import com.sceyt.chatuikit.data.models.messages.SystemMessageAction

data class SystemMessagesConfig(
    val enabled: Boolean = true,
    val groupCreated: Boolean = true,
    val channelCreated: Boolean = true,
    val memberAdded: Boolean = true,
    val memberRemoved: Boolean = true,
    val memberLeft: Boolean = true,
    val joinByInviteLink: Boolean = true,
    val disappearingMessageChanged: Boolean = true,
) {
    fun isEnabled(type: SystemMessageAction): Boolean {
        if (!enabled) return false

        return when (type) {
            SystemMessageAction.GroupCreated -> groupCreated
            SystemMessageAction.ChannelCreated -> channelCreated
            SystemMessageAction.MemberAdded -> memberAdded
            SystemMessageAction.MemberRemoved -> memberRemoved
            SystemMessageAction.MemberLeaved -> memberLeft
            SystemMessageAction.JoinByInviteLink -> joinByInviteLink
            SystemMessageAction.DisappearingMessage -> disappearingMessageChanged
        }
    }
}
