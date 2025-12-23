package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelInviteKeyData
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelInviteKeyLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.repositories.ChannelInviteKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PersistenceChannelInviteKeyLogicImpl(
    private val channelInviteKeyRepository: ChannelInviteKeyRepository,
    private val channelsLogic: PersistenceChannelsLogic,
) : PersistenceChannelInviteKeyLogic, SceytKoinComponent {

    override suspend fun getChannelInviteKeys(
        channelId: Long,
    ): SceytResponse<List<ChannelInviteKeyData>> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.getChannelInviteKeys(channelId)
    }

    override suspend fun getChannelInviteKey(
        channelId: Long,
        key: String,
    ): SceytResponse<ChannelInviteKeyData> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.getChannelInviteKey(channelId, key)
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
        deletePermanently: Boolean,
    ): SceytResponse<ChannelInviteKeyData> = withContext(Dispatchers.IO) {
        return@withContext channelInviteKeyRepository.regenerateChannelInviteKey(
            channelId = channelId,
            key = key,
            deletePermanently = deletePermanently
        ).onSuccessNotNull { data ->
            channelsLogic.checkChannelUrlUpdate(
                channelId = channelId,
                oldKey = key,
                newKey = data.key
            )
        }
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
        return@withContext channelInviteKeyRepository.deleteRevokedChannelInviteKeys(
            channelId,
            keys
        )
    }
}

