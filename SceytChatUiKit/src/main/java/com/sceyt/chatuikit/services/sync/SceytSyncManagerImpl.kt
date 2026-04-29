package com.sceyt.chatuikit.services.sync

import android.util.Log
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.database.dao.ChannelSyncStateDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelSyncStateEntity
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
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
    private val channelSyncStateDao: ChannelSyncStateDao,
) : SceytSyncManager {

    private var syncResultData: SyncResultData = SyncResultData()
    private var syncIsInProcess: AtomicBoolean = AtomicBoolean(false)
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
        if (syncIsInProcess.get())
            return

        val coroutineContext = createCoroutineContext().also { syncContext = it }
        withContext(coroutineContext) {
            syncIsInProcess.set(true)
            syncResultData = SyncResultData()
            val result = syncChannels(config)
            withContext(Dispatchers.Main) {
                syncResultCallbacks.forEach {
                    it(Result.success(result))
                }
            }
            syncResultCallbacks.clear()
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
        syncResultCallbacks.clear()
    }

    private suspend fun syncChannels(config: ChannelListConfig): SyncResultData {
        return coroutineScope {
            suspendCancellableCoroutine { cont ->
                launch(Dispatchers.IO) {
                    val syncChannelData = SceytSyncManager.SyncChannelData(mutableSetOf(), false)
                    channelInteractor.syncChannels(config).collect {
                        when (it) {
                            is SyncResult.Error -> {
                                syncChannelData.withError = true
                                cont.safeResume(syncResultData)
                            }

                            is SyncResult.Proportion -> {
                                val channels = it.items
                                syncChannelsMessages(channels)
                                syncChannelData.channels.addAll(channels)
                            }

                            SyncResult.SuccessfullyFinished -> {
                                SceytSyncManager.syncChannelsFinished_.tryEmit(syncChannelData)
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
        val lastSyncMessageId = channelSyncStateDao.getLastSyncedMessageId(channel.id)
        if (lastSyncMessageId == lastMessageId) {
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
        messageInteractor.syncMessagesAfterMessageId(
            conversationId = channel.id,
            replyInThread = false,
            messageId = fromMessageId
        ).collect { result ->
            when (result) {
                is SyncResult.Proportion -> {
                    if (syncConversation)
                        SceytSyncManager.syncChannelMessagesFinished_.tryEmit(channel to result.items)
                    syncResultData.syncedMessagesCount += result.items.size
                }

                SyncResult.SuccessfullyFinished -> {
                    channel.lastMessage?.let { message ->
                        channelSyncStateDao.upsertChannelSyncState(
                            ChannelSyncStateEntity(
                                channelId = channel.id,
                                lastSyncedMessageId = message.id
                            )
                        )
                    }
                }

                is SyncResult.Error -> Unit
            }
        }
    }

    private fun createCoroutineContext(): CoroutineContext {
        return Dispatchers.IO + Job()
    }
}