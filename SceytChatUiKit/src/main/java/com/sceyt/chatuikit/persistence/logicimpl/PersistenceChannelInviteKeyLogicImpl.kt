package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelInviteKeyData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelInviteKeyLogic
import com.sceyt.chatuikit.persistence.repositories.ChannelInviteKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PersistenceChannelInviteKeyLogicImpl(
        private val channelInviteKeyRepository: ChannelInviteKeyRepository,
) : PersistenceChannelInviteKeyLogic, SceytKoinComponent {

    override suspend fun getChannelByInviteKey(
            inviteKey: String,
    ): SceytResponse<SceytChannel> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.getChannelByInviteKey(inviteKey)
    }

    override suspend fun getChannelInviteKeys(
            channelId: Long,
    ): SceytResponse<List<ChannelInviteKeyData>> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.getChannelInviteKeys(channelId)
    }

    override suspend fun getChannelInviteKeySettings(
            channelId: Long,
            key: String,
    ): SceytResponse<ChannelInviteKeyData> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.getChannelInviteKeySettings(channelId, key)
    }

    override suspend fun createChannelInviteKey(
            channelId: Long,
            expireAt: Long,
            maxUses: Int,
            accessPriorHistory: Boolean,
    ): SceytResponse<ChannelInviteKeyData> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.createChannelInviteKey(
            channelId = channelId,
            expireAt = expireAt,
            maxUses = maxUses,
            accessPriorHistory = accessPriorHistory
        )
    }

    override suspend fun updateInviteKeySettings(
            channelId: Long,
            key: String,
            expireAt: Long,
            maxUses: Int,
            accessPriorHistory: Boolean,
    ): SceytResponse<Boolean> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.updateInviteKeySettings(
            channelId = channelId,
            key = key,
            expireAt = expireAt,
            maxUses = maxUses,
            accessPriorHistory = accessPriorHistory
        )
    }

    override suspend fun regenerateChannelInviteKey(
            channelId: Long,
            key: String,
    ): SceytResponse<ChannelInviteKeyData> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.regenerateChannelInviteKey(channelId, key)
    }

    override suspend fun revokeChannelInviteKeys(
            channelId: Long,
            keys: List<String>,
    ): SceytResponse<Boolean> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.revokeChannelInviteKeys(channelId, keys)
    }

    override suspend fun deleteRevokedChannelInviteKeys(
            channelId: Long,
            keys: List<String>,
    ): SceytResponse<Boolean> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.deleteRevokedChannelInviteKeys(channelId, keys)
    }
}

