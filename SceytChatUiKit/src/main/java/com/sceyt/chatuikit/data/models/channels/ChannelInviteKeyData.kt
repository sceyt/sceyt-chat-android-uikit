package com.sceyt.chatuikit.data.models.channels

data class ChannelInviteKeyData(
        val channelId: Long,
        val key: String,
        val expireAt: Long,
        val revokedAt: Long,
        val revoked: Boolean,
        val accessPriorHistory: Boolean,
        val maxUses: Int,
        val createdBy: SceytMember,
)