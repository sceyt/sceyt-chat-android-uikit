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
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChannelEventsObserverService {
    private val _onMessageFlow: MutableStateFlow<Pair<Channel, Message>?> = MutableStateFlow(null)
    val onMessageFlow: StateFlow<Pair<Channel, Message>?> = _onMessageFlow

    private val _onMessageStatusFlow: MutableStateFlow<MessageStatusChange?> = MutableStateFlow(null)
    val onMessageStatusFlow: StateFlow<MessageStatusChange?> = _onMessageStatusFlow

    private val _onMessageEditedOrDeletedChannel = kotlinx.coroutines.channels.Channel<Message?>()
    val onMessageEditedOrDeletedChannel: ReceiveChannel<Message?> = _onMessageEditedOrDeletedChannel

    private val _onMessageReactionUpdatedChannel = kotlinx.coroutines.channels.Channel<Message?>()
    val onMessageReactionUpdatedChannel: ReceiveChannel<Message?> = _onMessageReactionUpdatedChannel

    private val _onChannelClearedHistoryChannel = kotlinx.coroutines.channels.Channel<Channel?>()
    val onChannelClearedHistoryChannel: ReceiveChannel<Channel?> = _onChannelClearedHistoryChannel

    init {
        ChatClient.getClient().addMessageListener(TAG, object : MessageListener {

            override fun onMessage(channel: Channel, message: Message) {
                _onMessageFlow.tryEmit(Pair(channel, message))

                ClientWrapper.markMessagesAsDisplayed(channel.id, longArrayOf(message.id)) { _, _ ->
                }
            }

            override fun onMessageDeleted(message: Message?) {
                _onMessageEditedOrDeletedChannel.trySend(message)
            }

            override fun onMessageEdited(message: Message?) {
                _onMessageEditedOrDeletedChannel.trySend(message)
            }

            override fun onReactionAdded(message: Message?, reaction: Reaction?) {
                _onMessageReactionUpdatedChannel.trySend(message)
            }

            override fun onReactionDeleted(message: Message?, reaction: Reaction?) {
                _onMessageReactionUpdatedChannel.trySend(message)
            }
        })

        ChatClient.getClient().addChannelListener(TAG, object : ChannelListener {

            override fun onDeliveryReceiptReceived(channel: Channel?, from: User?, messageIds: MutableList<Long>) {
                _onMessageStatusFlow.tryEmit(MessageStatusChange(channel, from, DeliveryStatus.Delivered, messageIds))
            }

            override fun onReadReceiptReceived(channel: Channel?, from: User?, messageIds: MutableList<Long>) {
                _onMessageStatusFlow.tryEmit(MessageStatusChange(channel, from, DeliveryStatus.Read, messageIds))
            }

            override fun onClearedHistory(channel: Channel?) {
                _onChannelClearedHistoryChannel.trySend(channel)
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