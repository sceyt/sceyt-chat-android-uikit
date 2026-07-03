package com.sceyt.chatuikit.services.sync

import android.util.Log
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.presentation.common.collections.ConcurrentHashSet
import com.sceyt.chatuikit.services.sync.SceytSyncManager.SyncResultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

internal class SceytSyncManagerImpl(
    private val channelInteractor: ChannelInteractor,
    private val messageInteractor: MessageInteractor,
    private val channelSyncStateStore: ChannelSyncStateStore,
) : SceytSyncManager {

    private var syncResultData: SyncResultData = SyncResultData()
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
                syncResultData = SyncResultData()
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

    override suspend fun syncConversationMessagesAfter(channelId: Long, fromMessageId: Long) {
        val response = channelInteractor.getChannelFromServer(channelId)
        if (response is SceytResponse.Success && response.data != null)
            syncMessagesAfter(response.data, fromMessageId, true)
    }

    override fun cancelSync() {
        syncContext?.cancel()
        syncContext = null
        syncResultCallbacks.clear()
        syncIsInProcess.set(false)
    }

    private suspend fun syncChannels(config: ChannelListConfig): SyncResultData {
        return coroutineScope {
            suspendCancellableCoroutine { cont ->
                launch(Dispatchers.IO) {
                    channelInteractor.syncChannels(config).collect { result ->
                        when (result) {
                            is SyncResult.Error -> {
                                SceytSyncManager.syncChannelsResult_.tryEmit(result)
                                cont.safeResume(syncResultData)
                            }

                            is SyncResult.Proportion -> {
                                SceytSyncManager.syncChannelsResult_.tryEmit(result)
                                syncChannelsMessages(result.items)
                            }

                            SyncResult.SuccessfullyFinished -> {
                                SceytSyncManager.syncChannelsResult_.tryEmit(result)
                                cont.safeResume(syncResultData)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun syncChannelsMessages(list: List<SceytChannel>) {
        list.forEach {
            if (it.newMessageCount > 0) {
                syncResultData.apply {
                    if (it.muted) {
                        unreadMessagesImMutedChannelCount += it.newMessageCount.toInt()
                        unreadMutedChannelsCount++
                    }
                    totalUnreadMessagesCount += it.newMessageCount.toInt()
                    totalUnreadChannelsCount++
                }
            }
            loadMessages(it)
        }
        syncResultData.syncedChannelsCount += list.size
    }

    private suspend fun loadMessages(channel: SceytChannel) {
        if (channel.lastMessage == null || channel.lastDisplayedMessageId == channel.lastMessage.id)
            return

        val lastMessageId = channel.lastMessage.id
        if (channelSyncStateStore.isMessagesSynced(channel.id, lastMessageId)) {
            Log.d(TAG, "Messages are up to date, skipping sync for channel ${channel.id}")
            return
        }

        Log.d(TAG, "Syncing messages for channel ${channel.id} after message id ${channel.lastDisplayedMessageId}")
        syncMessagesAfter(channel, channel.lastDisplayedMessageId, false)
    }

    private suspend fun syncMessagesAfter(
        channel: SceytChannel,
        fromMessageId: Long,
        syncConversation: Boolean
    ) {
        val syncedConversationMessages = mutableListOf<SceytMessage>()

        fun emitSyncedConversationMessages() {
            if (syncConversation && syncedConversationMessages.isNotEmpty()) {
                SceytSyncManager.syncChannelMessagesFinished_.tryEmit(
                    channel to syncedConversationMessages.toList()
                )
                syncedConversationMessages.clear()
            }
        }

        messageInteractor.syncMessagesAfterMessageId(
            conversationId = channel.id,
            replyInThread = false,
            messageId = fromMessageId
        ).collect { result ->
            when (result) {
                is SyncResult.Proportion -> {
                    if (syncConversation)
                        syncedConversationMessages.addAll(result.items)
                    syncResultData.syncedMessagesCount += result.items.size
                }

                SyncResult.SuccessfullyFinished -> {
                    emitSyncedConversationMessages()
                    channel.lastMessage?.let { message ->
                        channelSyncStateStore.updateSyncState(channel.id, message.id)
                    }
                }

                is SyncResult.Error -> {
                    emitSyncedConversationMessages()
                }
            }
        }
    }

    private fun createCoroutineContext(): CoroutineContext {
        return Dispatchers.IO + Job()
    }
}
