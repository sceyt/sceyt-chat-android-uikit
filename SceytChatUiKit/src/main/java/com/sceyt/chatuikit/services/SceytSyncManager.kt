package com.sceyt.chatuikit.services

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelsCache
import com.sceyt.chatuikit.persistence.shared.LiveEvent
import com.sceyt.chatuikit.presentation.common.ConcurrentHashSet
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig.CHANNELS_LOAD_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class SceytSyncManager(private val channelInteractor: ChannelInteractor,
                       private val messageInteractor: MessageInteractor,
                       private val channelsCache: ChannelsCache) : SceytKoinComponent {

    private var syncResultData: SyncResultData = SyncResultData()

    @Volatile
    private var syncIsInProcess: Boolean = false
    private val syncResultCallbacks = ConcurrentHashSet<(Result<SyncResultData>) -> Unit>()

    companion object {
        private val syncChannelsFinished_ = LiveEvent<SyncChannelData>()
        val syncChannelsFinished = syncChannelsFinished_.asLiveData()
        private val syncChannelMessagesFinished_ = LiveEvent<Pair<SceytChannel, List<SceytMessage>>>()
        val syncChannelMessagesFinished = syncChannelMessagesFinished_.asLiveData()
    }

    suspend fun startSync(force: Boolean, resultCallback: ((Result<SyncResultData>) -> Unit)? = null) {
        resultCallback?.let { syncResultCallbacks.add(it) }

        if (syncIsInProcess)
            return

        if (!channelsCache.initialized && !force) {
            val errorMessage = "Ui kit still not have loaded channels to sync, no need to sync"
            finishSyncWithError(Exception(errorMessage))
            SceytLog.e(TAG, errorMessage)
            return
        }

        withContext(Dispatchers.IO) {
            syncIsInProcess = true
            syncResultData = SyncResultData()
            val result = getChannels()
            withContext(Dispatchers.Main) {
                syncResultCallbacks.forEach {
                    it(Result.success(result))
                }
            }
            syncResultCallbacks.clear()
            syncIsInProcess = false
        }
    }

    suspend fun syncConversationMessagesAfter(channelId: Long, fromMessageId: Long) {
        val response = channelInteractor.getChannelFromServer(channelId)
        if (response is SceytResponse.Success && response.data != null)
            syncMessagesAfter(response.data, fromMessageId, true)
    }

    private suspend fun getChannels(): SyncResultData {
        return coroutineScope {
            suspendCancellableCoroutine { cont ->
                launch(Dispatchers.IO) {
                    val syncChannelData = SyncChannelData(mutableSetOf(), false)
                    channelInteractor.syncChannels(CHANNELS_LOAD_SIZE).collect {
                        when (it) {
                            is GetAllChannelsResponse.Error -> {
                                syncChannelData.withError = true
                                cont.resume(syncResultData)
                            }

                            is GetAllChannelsResponse.Proportion -> {
                                val channels = it.channels
                                syncChannelsMessages(channels)
                                syncChannelData.channels.addAll(channels)
                            }

                            GetAllChannelsResponse.SuccessfullyFinished -> {
                                syncChannelsFinished_.postValue(syncChannelData)
                                cont.resume(syncResultData)
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
        if (channel.lastMessage == null || channel.lastDisplayedMessageId == channel.lastMessage?.id)
            return

        syncMessagesAfter(channel, channel.lastDisplayedMessageId, false)
    }

    private suspend fun syncMessagesAfter(channel: SceytChannel, fromMessageId: Long, syncConversation: Boolean) {
        messageInteractor.syncMessagesAfterMessageId(channel.id, false, fromMessageId).collect {
            if (it is SceytResponse.Success)
                it.data?.let { messages ->
                    if (syncConversation)
                        syncChannelMessagesFinished_.postValue(Pair(channel, messages))
                    syncResultData.syncedMessagesCount += messages.size
                }
        }
    }

    private fun finishSyncWithError(exception: java.lang.Exception) {
        syncResultCallbacks.forEach {
            it(Result.failure(exception))
        }
        syncResultCallbacks.clear()
    }

    data class SyncChannelData(
            val channels: MutableSet<SceytChannel>,
            var withError: Boolean
    )

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