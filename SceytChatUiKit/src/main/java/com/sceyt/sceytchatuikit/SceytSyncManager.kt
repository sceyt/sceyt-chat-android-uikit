package com.sceyt.sceytchatuikit

import androidx.lifecycle.LiveData
import com.hadilq.liveevent.LiveEvent
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.presentation.common.ConcurrentHashSet
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.CHANNELS_LOAD_SIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class SceytSyncManager(private val channelsMiddleWare: PersistenceChanelMiddleWare,
                       private val messagesMiddleWare: PersistenceMessagesMiddleWare) : SceytKoinComponent, CoroutineScope {

    private var syncResultData: SyncResultData = SyncResultData()
    @Volatile
    private var syncIsInProcess: Boolean = false
    private val syncResultCallbacks = ConcurrentHashSet<(SyncResultData) -> Unit>()

    companion object {
        private val syncChannelsFinished_ = LiveEvent<SyncChannelData>()
        val syncChannelsFinished: LiveData<SyncChannelData> = syncChannelsFinished_
        private val syncChannelMessagesFinished_ = LiveEvent<Pair<SceytChannel, List<SceytMessage>>>()
        val syncChannelMessagesFinished: LiveData<Pair<SceytChannel, List<SceytMessage>>> = syncChannelMessagesFinished_
    }

    fun startSync(resultCallback: ((SyncResultData) -> Unit)? = null) {
        resultCallback?.let { syncResultCallbacks.add(it) }
        if (syncIsInProcess)
            return

        launch {
            syncIsInProcess = true
            syncResultData = SyncResultData()
            val result = getChannels()
            syncResultCallbacks.forEach {
                it(result)
            }
            syncResultCallbacks.clear()
            syncIsInProcess = false
        }
    }

    suspend fun syncConversationMessagesAfter(channelId: Long, fromMessageId: Long) {
        val response = channelsMiddleWare.getChannelFromServer(channelId)
        if (response is SceytResponse.Success && response.data != null)
            syncMessagesAfter(response.data, fromMessageId, true)
    }

    private suspend fun getChannels(): SyncResultData {
        return coroutineScope {
            suspendCancellableCoroutine { cont ->
                launch {
                    val syncChannelData = SyncChannelData(mutableSetOf(), false)
                    channelsMiddleWare.syncChannels(CHANNELS_LOAD_SIZE)
                        .onCompletion {
                            syncChannelsFinished_.postValue(syncChannelData)
                            cont.resume(syncResultData)
                        }.collect {
                            if (it is SceytResponse.Success) {
                                it.data?.let { channels ->
                                    syncChannelsMessages(channels)
                                    syncChannelData.channels.addAll(channels)
                                }
                            } else syncChannelData.withError = true
                        }
                }
            }
        }
    }

    private suspend fun syncChannelsMessages(list: List<SceytChannel>) {
        list.forEach {
            if (it.unreadMessageCount > 0) {
                syncResultData.apply {
                    unreadMessagesCount += it.unreadMessageCount.toInt()
                    unreadChannelsCount++
                }
            }
            loadMessages(it)
        }
        syncResultData.syncedChannelsCount += list.size
    }

    private suspend fun loadMessages(channel: SceytChannel) {
        if (channel.lastReadMessageId == channel.lastMessage?.id)
            return

        syncMessagesAfter(channel, channel.lastReadMessageId, false)
    }

    private suspend fun syncMessagesAfter(channel: SceytChannel, fromMessageId: Long, syncConversation: Boolean) {
        messagesMiddleWare.syncMessagesAfterMessageId(channel.id, false, fromMessageId).collect {
            if (it is SceytResponse.Success)
                it.data?.let { messages ->
                    if (syncConversation)
                        syncChannelMessagesFinished_.postValue(Pair(channel, messages))
                    syncResultData.syncedMessagesCount += messages.size
                }
        }
    }

    data class SyncChannelData(
            val channels: MutableSet<SceytChannel>,
            var withError: Boolean
    )

    data class SyncResultData(
            var unreadChannelsCount: Int = 0,
            var unreadMessagesCount: Int = 0,
            var syncedChannelsCount: Int = 0,
            var syncedMessagesCount: Int = 0
    ) {
        override fun toString(): String {
            return "unreadChannelsCount-> $unreadChannelsCount, unreadMessagesCount-> $unreadMessagesCount," +
                    "syncedChannelsCount-> $syncedChannelsCount, syncedMessagesCount-> $syncedMessagesCount"
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}