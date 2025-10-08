package com.sceyt.chatuikit.data.models.channels

import com.sceyt.chatuikit.data.models.messages.SceytUser

data class ChannelInviteKeyData(
        val channelId: Long,
        val key: String,
        val expireAt: Long,
        val maxUses: Int,
        val createdBy: SceytUser,
)