package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelInviteKeyData
import com.sceyt.chatuikit.data.models.channels.SceytChannel

interface ChannelInviteKeyRepository {
    suspend fun getChannelByInviteKey(inviteKey: String): SceytResponse<SceytChannel>
    suspend fun getChannelInviteKeys(channelId: Long): SceytResponse<List<ChannelInviteKeyData>>
    suspend fun createChannelInviteKey(
            channelId: Long,
            expireAt: Long,
            maxUses: Int,
            accessPriorHistory: Boolean,
    ): SceytResponse<ChannelInviteKeyData>
    suspend fun regenerateChannelInviteKey(channelId: Long, key: String): SceytResponse<ChannelInviteKeyData>
    suspend fun revokeChannelInviteKeys(channelId: Long, keys: List<String>): SceytResponse<Boolean>
    suspend fun deleteRevokedChannelInviteKeys(channelId: Long, keys: List<String>): SceytResponse<Boolean>
}