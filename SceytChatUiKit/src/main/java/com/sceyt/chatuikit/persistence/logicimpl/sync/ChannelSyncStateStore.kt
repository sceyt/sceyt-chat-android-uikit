package com.sceyt.chatuikit.persistence.logicimpl.sync

import com.sceyt.chatuikit.persistence.database.dao.ChannelSyncStateDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelSyncStateEntity

internal class ChannelSyncStateStore(
    private val dao: ChannelSyncStateDao
) {

    suspend fun isMessagesSynced(channelId: Long, lastMessageId: Long): Boolean {
        return dao.getLastSyncedMessageId(channelId) == lastMessageId
    }

    suspend fun updateSyncState(channelId: Long, lastSyncedMessageId: Long) {
        dao.upsertChannelSyncState(
            ChannelSyncStateEntity(
                channelId = channelId,
                lastSyncedMessageId = lastSyncedMessageId
            )
        )
    }

    suspend fun updateSyncStateForMessage(channelId: Long, messageId: Long) {
        updateSyncState(channelId, messageId)
    }

    suspend fun deleteSyncState(channelId: Long) {
        dao.deleteChannelSyncState(channelId)
    }

    suspend fun deleteSyncStates(channelIds: List<Long>) {
        dao.deleteChannelSyncStates(channelIds)
    }
}
