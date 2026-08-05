package com.sceyt.chatuikit.services.sync

import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.presentation.common.collections.ConcurrentHashSet
import com.sceyt.chatuikit.services.sync.SceytSyncManager.SyncResultData
import com.sceyt.chatuikit.services.sync.SceytSyncManager.SyncedConversationMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

internal class SceytSyncManagerImpl(
    private val channelInteractor: ChannelInteractor,
    private val messageInteractor: MessageInteractor,
    private val channelSyncStateStore: ChannelSyncStateStore,
) : SceytSyncManager {

    private var syncResultData: SyncResultData = SyncResultData()
    private val syncResultDataMutex = Mutex()
    private val syncIsInProcess: AtomicBoolean = AtomicBoolean(false)
    private val syncResultCallbacks = ConcurrentHashSet<(Result<SyncResultData>) -> Unit>()
    private var syncContext: CoroutineContext? = null

    companion object {
        private const val TAG = "SceytSyncManager"
    }

    override suspend fun startSync(
        config: ChannelListConfig,
        resultCallback: ((Result<SyncResultData>) -> Unit)?
    ) {
        resultCallback?.let { syncResultCallbacks.add(it) }
        if (!syncIsInProcess.compareAndSet(false, true))
            return

        val coroutineContext = createCoroutineContext().also { syncContext = it }
        try {
            withContext(coroutineContext) {
                resetSyncResultData()
                val result = syncChannels(config)
                withContext(Dispatchers.Main) {
                    syncResultCallbacks.forEach {
                        it(Result.success(result))
                    }
                }
            }
        } finally {
            syncResultCallbacks.clear()
            syncContext = null
            syncIsInProcess.set(false)
        }
    }

    override suspend fun syncConversationMessagesAfter(
        channelId: Long,
        fromMessageId: Long
    ): SyncedConversationMessages? = withContext(Dispatchers.IO) {
        val response = channelInteractor.getChannelFromServer(channelId)
        return@withContext if (response is SceytResponse.Success && response.data != null)
            syncConversationMessagesAfter(response.data, fromMessageId)
        else null
    }

    override fun cancelSync() {
        syncContext?.cancel()
        syncContext = null
        syncResultCallbacks.clear()
        syncIsInProcess.set(false)
    }

    private suspend fun syncChannels(config: ChannelListConfig): SyncResultData {
        channelInteractor.syncChannels(config).collect { result ->
            when (result) {
                is SyncResult.Error -> {
                    SceytSyncManager.syncChannelsResult_.tryEmit(result)
                }

                is SyncResult.Proportion -> {
                    SceytSyncManager.syncChannelsResult_.tryEmit(result)
                    syncChannelsMessages(result.items)
                }

                SyncResult.SuccessfullyFinished -> {
                    SceytSyncManager.syncChannelsResult_.tryEmit(result)
                }
            }
        }
        return syncResultDataSnapshot()
    }

    private suspend fun syncChannelsMessages(list: List<SceytChannel>) = coroutineScope {
        var totalUnreadMessagesCount = 0
        var totalUnreadChannelsCount = 0
        var unreadMutedChannelsCount = 0
        var unreadMessagesImMutedChannelCount = 0

        list.forEach { channel ->
            if (channel.newMessageCount > 0) {
                val newMessageCount = channel.newMessageCount.toInt()
                if (channel.muted) {
                    unreadMessagesImMutedChannelCount += newMessageCount
                    unreadMutedChannelsCount++
                }
                totalUnreadMessagesCount += newMessageCount
                totalUnreadChannelsCount++
            }
        }

        updateSyncResultData {
            it.copy(
                totalUnreadMessagesCount = it.totalUnreadMessagesCount + totalUnreadMessagesCount,
                totalUnreadChannelsCount = it.totalUnreadChannelsCount + totalUnreadChannelsCount,
                unreadMutedChannelsCount = it.unreadMutedChannelsCount + unreadMutedChannelsCount,
                unreadMessagesImMutedChannelCount = it.unreadMessagesImMutedChannelCount + unreadMessagesImMutedChannelCount,
                syncedChannelsCount = it.syncedChannelsCount + list.size
            )
        }

        val syncedMessagesCount = list
            .map { channel ->
                async(Dispatchers.IO) {
                    loadMessages(channel)
                }
            }
            .awaitAll()
            .sum()

        addSyncedMessagesCount(syncedMessagesCount)
    }

    private suspend fun loadMessages(channel: SceytChannel): Int {
        if (channel.lastMessage == null || channel.lastDisplayedMessageId == channel.lastMessage.id)
            return 0

        val lastMessageId = channel.lastMessage.id
        if (channelSyncStateStore.isMessagesSynced(channel.id, lastMessageId)) {
            return 0
        }

        SceytLog.d(
            TAG,
            "Syncing messages for channel ${channel.id} after message id ${channel.lastDisplayedMessageId}"
        )
        return syncMessagesAfter(channel, channel.lastDisplayedMessageId)
    }

    private suspend fun syncMessagesAfter(
        channel: SceytChannel,
        fromMessageId: Long
    ): Int {
        var syncedMessagesCount = 0

        messageInteractor.syncMessagesAfterMessageId(
            conversationId = channel.id,
            replyInThread = false,
            messageId = fromMessageId
        ).collect { result ->
            when (result) {
                is SyncResult.Proportion -> {
                    syncedMessagesCount += result.items.size
                }

                SyncResult.SuccessfullyFinished -> {
                    channel.lastMessage?.let { message ->
                        channelSyncStateStore.updateSyncState(channel.id, message.id)
                    }
                }

                is SyncResult.Error -> Unit
            }
        }

        return syncedMessagesCount
    }

    private suspend fun syncConversationMessagesAfter(
        channel: SceytChannel,
        fromMessageId: Long
    ): SyncedConversationMessages? {
        val syncedConversationMessages = mutableListOf<SceytMessage>()

        messageInteractor.syncMessagesAfterMessageId(
            conversationId = channel.id,
            replyInThread = false,
            messageId = fromMessageId
        ).collect { result ->
            when (result) {
                is SyncResult.Proportion -> {
                    syncedConversationMessages.addAll(result.items)
                }

                SyncResult.SuccessfullyFinished -> {
                    channel.lastMessage?.let { message ->
                        channelSyncStateStore.updateSyncState(channel.id, message.id)
                    }
                }

                is SyncResult.Error -> Unit
            }
        }

        return if (syncedConversationMessages.isNotEmpty()) {
            SyncedConversationMessages(
                channel = channel,
                messages = syncedConversationMessages,
                fromMessageId = fromMessageId
            )
        } else null
    }

    private fun createCoroutineContext(): CoroutineContext {
        return Dispatchers.IO + Job()
    }

    private suspend fun resetSyncResultData() {
        syncResultDataMutex.withLock {
            syncResultData = SyncResultData()
        }
    }

    private suspend fun addSyncedMessagesCount(count: Int) {
        updateSyncResultData {
            it.copy(syncedMessagesCount = it.syncedMessagesCount + count)
        }
    }

    private suspend fun updateSyncResultData(update: (SyncResultData) -> SyncResultData) {
        syncResultDataMutex.withLock {
            syncResultData = update(syncResultData)
        }
    }

    private suspend fun syncResultDataSnapshot(): SyncResultData {
        return syncResultDataMutex.withLock {
            syncResultData
        }
    }
}
