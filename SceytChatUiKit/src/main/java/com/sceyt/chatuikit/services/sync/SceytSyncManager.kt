package com.sceyt.chatuikit.services.sync

import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.broadcastSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface SceytSyncManager {

    suspend fun startSync(
        config: ChannelListConfig,
        resultCallback: ((Result<SyncResultData>) -> Unit)? = null
    )

    suspend fun syncConversationMessagesAfter(
        channelId: Long,
        fromMessageId: Long
    ): SyncedConversationMessages?

    fun cancelSync()

    companion object {
        /**
         * Emits the channel-sync lifecycle (proportions, finished, error), so the UI can refresh the
         * loaded window incrementally instead of waiting for the whole sync to complete.
         */
        internal val syncChannelsResult_ = broadcastSharedFlow<SyncResult<SceytChannel>>(
            extraBufferCapacity = 8
        )
        val syncChannelsResult = syncChannelsResult_.asSharedFlow()
    }

    data class SyncedConversationMessages(
        val channel: SceytChannel,
        val messages: List<SceytMessage>,
        val fromMessageId: Long,
    )

    /**@param totalUnreadChannelsCount is total unread channels count, include muted channels.
     * @param totalUnreadMessagesCount is total unread messages count, include messages in muted channels.
     * @param unreadMutedChannelsCount is total unread muted channels count.
     * @param unreadMessagesImMutedChannelCount is total unread messages in muted channels count.
     * @param syncedChannelsCount is total synced channels count, include muted channels.
     * @param syncedMessagesCount is total synced messages count, include messages in muted channels.*/
    data class SyncResultData(
        val totalUnreadChannelsCount: Int = 0,
        val totalUnreadMessagesCount: Int = 0,
        val unreadMutedChannelsCount: Int = 0,
        val unreadMessagesImMutedChannelCount: Int = 0,
        val syncedChannelsCount: Int = 0,
        val syncedMessagesCount: Int = 0,
    ) {
        override fun toString(): String {
            return "unreadChannelsCount-> $totalUnreadChannelsCount, unreadMutedChannelsCount-> $unreadMutedChannelsCount " +
                    "unreadMessagesCount-> $totalUnreadMessagesCount, unreadMessagesImMutedChannelCount $unreadMessagesImMutedChannelCount" +
                    "syncedChannelsCount-> $syncedChannelsCount, syncedMessagesCount-> $syncedMessagesCount"
        }
    }
}
