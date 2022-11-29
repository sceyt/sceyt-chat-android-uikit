package com.sceyt.sceytchatuikit

import androidx.lifecycle.LiveData
import com.hadilq.liveevent.LiveEvent
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.CHANNELS_LOAD_SIZE
import kotlinx.coroutines.flow.onCompletion

class SceytSyncManager(private val channelsMiddleWare: PersistenceChanelMiddleWare,
                       private val messagesMiddleWare: PersistenceMessagesMiddleWare) : SceytKoinComponent {

    companion object {
        private val syncChannelsFinished_ = LiveEvent<SyncChannelData>()
        val syncChannelsFinished: LiveData<SyncChannelData> = syncChannelsFinished_
        private val syncChannelMessagesFinished_ = LiveEvent<Pair<SceytChannel, List<SceytMessage>>>()
        val syncChannelMessagesFinished: LiveData<Pair<SceytChannel, List<SceytMessage>>> = syncChannelMessagesFinished_
    }

    suspend fun startSync() {
        getChannels()
    }

    suspend fun syncConversationMessagesAfter(channelId: Long, fromMessageId: Long) {
        val response = channelsMiddleWare.getChannelFromServer(channelId)
        if (response is SceytResponse.Success && response.data != null)
            syncMessagesAfter(response.data, fromMessageId, true)
    }

    private suspend fun getChannels() {
        val syncChannelData = SyncChannelData(mutableSetOf(), false)
        channelsMiddleWare.syncChannels(CHANNELS_LOAD_SIZE)
            .onCompletion {
                syncChannelsFinished_.postValue(syncChannelData)
            }.collect {
                if (it is SceytResponse.Success) {
                    it.data?.let { channels ->
                        syncChannelsMessages(channels)
                        syncChannelData.channels.addAll(channels)
                    }
                } else syncChannelData.withError = true
            }
    }

    private suspend fun syncChannelsMessages(list: List<SceytChannel>) {
        list.forEach {
            loadMessages(it)
        }
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
                }
        }
    }

    data class SyncChannelData(
            val channels: MutableSet<SceytChannel>,
            var withError: Boolean
    )
}