package com.sceyt.chatuikit.services.sync

import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.broadcastSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface SceytSyncManager {

    suspend fun startSync(
        config: ChannelListConfig,
        resultCallback: ((Result<SyncResultData>) -> Unit)? = null
    )

    suspend fun syncConversationMessagesAfter(channelId: Long, fromMessageId: Long)

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

        internal val syncChannelMessagesFinished_ =
            broadcastSharedFlow<Pair<SceytChannel, List<SceytMessage>>>()

        val syncChannelMessagesFinished: SharedFlow<Pair<SceytChannel, List<SceytMessage>>> =
            syncChannelMessagesFinished_.asSharedFlow()
    }

    /**@param totalUnreadChannelsCount is total unread channels count, include muted channels.
     * @param totalUnreadMessagesCount is total unread messages count, include messages in muted channels.
     * @param unreadMutedChannelsCount is total unread muted channels count.
     * @param unreadMessagesImMutedChannelCount is total unread messages in muted channels count.
     * @param syncedChannelsCount is total synced channels count, include muted channels.
     * @param syncedMessagesCount is total synced messages count, include messages in muted channels.*/
    data class SyncResultData(
        var totalUnreadChannelsCount: Int = 0,
        var totalUnreadMessagesCount: Int = 0,
        var unreadMutedChannelsCount: Int = 0,
        var unreadMessagesImMutedChannelCount: Int = 0,
        var syncedChannelsCount: Int = 0,
        var syncedMessagesCount: Int = 0,
    ) {
        override fun toString(): String {
            return "unreadChannelsCount-> $totalUnreadChannelsCount, unreadMutedChannelsCount-> $unreadMutedChannelsCount " +
                    "unreadMessagesCount-> $totalUnreadMessagesCount, unreadMessagesImMutedChannelCount $unreadMessagesImMutedChannelCount" +
                    "syncedChannelsCount-> $syncedChannelsCount, syncedMessagesCount-> $syncedMessagesCount"
        }
    }
}
