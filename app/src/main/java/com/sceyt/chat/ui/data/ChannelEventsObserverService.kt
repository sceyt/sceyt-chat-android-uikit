package com.sceyt.chat.ui.data

import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_listeners.ChannelListener
import com.sceyt.chat.sceyt_listeners.MessageListener
import com.sceyt.chat.ui.extensions.TAG
import kotlinx.coroutines.flow.MutableStateFlow

class ChannelEventsObserverService {
    val onMessageFlow: MutableStateFlow<Pair<Channel, Message>?> = MutableStateFlow(null)
    val onMessageStatusFlow: MutableStateFlow<MessageStatusChange?> = MutableStateFlow(null)
    val onMessageEditedOrDeletedChannel = kotlinx.coroutines.channels.Channel<Message?>()
    val onMessageReactionUpdatedChannel = kotlinx.coroutines.channels.Channel<Message?>()
    val onChannelClearedHistoryChannel = kotlinx.coroutines.channels.Channel<Channel?>()

    init {
        ChatClient.getClient().addMessageListener(TAG, object : MessageListener {

            override fun onMessage(channel: Channel, message: Message) {
                onMessageFlow.tryEmit(Pair(channel, message))

                ClientWrapper.markMessagesAsDisplayed(channel.id, longArrayOf(message.id)) { _, _ ->
                }
            }

            override fun onMessageDeleted(message: Message?) {
                onMessageEditedOrDeletedChannel.trySend(message)
            }

            override fun onMessageEdited(message: Message?) {
                onMessageEditedOrDeletedChannel.trySend(message)
            }

            override fun onReactionAdded(message: Message?, reaction: Reaction?) {
                onMessageReactionUpdatedChannel.trySend(message)
            }

            override fun onReactionDeleted(message: Message?, reaction: Reaction?) {
                onMessageReactionUpdatedChannel.trySend(message)
            }
        })

        ChatClient.getClient().addChannelListener(TAG, object : ChannelListener {

            override fun onDeliveryReceiptReceived(channel: Channel?, from: User?, messageIds: MutableList<Long>) {
                onMessageStatusFlow.tryEmit(MessageStatusChange(channel, from, DeliveryStatus.Delivered, messageIds))
            }

            override fun onReadReceiptReceived(channel: Channel?, from: User?, messageIds: MutableList<Long>) {
                onMessageStatusFlow.tryEmit(MessageStatusChange(channel, from, DeliveryStatus.Read, messageIds))
            }

            override fun onClearedHistory(channel: Channel?) {
                onChannelClearedHistoryChannel.trySend(channel)
            }

            override fun onChannelUpdated(channel: Channel?) {
                /*channel?.let {
                    channelLiveData.postValue(it)
                }*/
            }
        })
    }

    data class MessageStatusChange(
            val channel: Channel?,
            val from: User?,
            val status: DeliveryStatus,
            val messageIds: MutableList<Long>
    )
}