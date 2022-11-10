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
import org.koin.core.component.inject

class SceytSyncManager : SceytKoinComponent {
    private val channelsMiddleWare: PersistenceChanelMiddleWare by inject()
    private val messagesMiddleWare: PersistenceMessagesMiddleWare by inject()

    companion object {
        private val syncChannelsFinished_ = LiveEvent<SyncChannelData>()
        val syncChannelsFinished: LiveData<SyncChannelData> = syncChannelsFinished_
        private val syncChannelMessagesFinished_ = LiveEvent<Pair<SceytChannel, List<SceytMessage>>>()
        val syncChannelMessagesFinished: LiveData<Pair<SceytChannel, List<SceytMessage>>> = syncChannelMessagesFinished_
    }

    suspend fun startSync() {
        getChannels()
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
        if (channel.lastReadMessageId == channel.lastMessage?.id) {
            return
        }

        messagesMiddleWare.syncMessagesAfterMessageId(channel.id, false, channel.lastReadMessageId).collect {
            if (it is SceytResponse.Success)
                it.data?.let { messages ->
                    syncChannelMessagesFinished_.postValue(Pair(channel, messages))
                }
        }
    }

    data class SyncChannelData(
            val channels: MutableSet<SceytChannel>,
            var withError: Boolean
    )
}