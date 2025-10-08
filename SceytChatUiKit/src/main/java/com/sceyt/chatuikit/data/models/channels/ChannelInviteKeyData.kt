package com.sceyt.chatuikit.data.models.channels

data class ChannelInviteKeyData(
        val channelId: Long,
        val key: String,
        val expireAt: Long,
        val maxUses: Int,
        val createdBy: SceytMember,
)