package com.sceyt.chat.ui.data.channeleventobserverservice

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterNotNull

object ChannelEventsObserverService {
    private val onMessageFlow_: MutableStateFlow<Pair<Channel, Message>?> = MutableStateFlow(null)
    val onMessageFlow: Flow<Pair<Channel, Message>?> = onMessageFlow_.filterNotNull()

    private val onMessageCleanFlow_: MutableStateFlow<Pair<Channel, Message>?> = MutableStateFlow(null)
    val onMessageCleanFlow: SharedFlow<Pair<Channel, Message>?> = onMessageCleanFlow_
        get() {
            onMessageCleanFlow_.value = null
            return field
        }

    private val onMessageStatusFlow_: MutableStateFlow<MessageStatusChange?> = MutableStateFlow(null)
    val onMessageStatusFlow: Flow<MessageStatusChange?> = onMessageStatusFlow_.filterNotNull()

    private val onMessageEditedOrDeletedChannel_ = kotlinx.coroutines.channels.Channel<Message?>()
    val onMessageEditedOrDeletedChannel: ReceiveChannel<Message?> = onMessageEditedOrDeletedChannel_

    private val onMessageReactionUpdatedChannel_ = kotlinx.coroutines.channels.Channel<Message?>()
    val onMessageReactionUpdatedChannel: ReceiveChannel<Message?> = onMessageReactionUpdatedChannel_

    private val onChannelEventChannel_ = kotlinx.coroutines.channels.Channel<ChannelEventData>()
    val onChannelEventChannel: ReceiveChannel<ChannelEventData> = onChannelEventChannel_

    init {

        ChatClient.getClient().addMessageListener(TAG, object : MessageListener {

            override fun onMessage(channel: Channel, message: Message) {
                onMessageFlow_.tryEmit(Pair(channel, message))
                onMessageCleanFlow_.tryEmit(Pair(channel, message))

                ClientWrapper.markMessagesAsReceived(channel.id, longArrayOf(message.id)) { _, _ ->
                }
            }

            override fun onMessageDeleted(message: Message?) {
                onMessageEditedOrDeletedChannel_.trySend(message)
            }

            override fun onMessageEdited(message: Message?) {
                onMessageEditedOrDeletedChannel_.trySend(message)
            }

            override fun onReactionAdded(message: Message?, reaction: Reaction?) {
                onMessageReactionUpdatedChannel_.trySend(message)
            }

            override fun onReactionDeleted(message: Message?, reaction: Reaction?) {
                onMessageReactionUpdatedChannel_.trySend(message)
            }
        })

        ChatClient.getClient().addChannelListener(TAG, object : ChannelListener {

            override fun onDeliveryReceiptReceived(channel: Channel?, from: User?, messageIds: MutableList<Long>) {
                onMessageStatusFlow_.tryEmit(MessageStatusChange(channel, from, DeliveryStatus.Delivered, messageIds))
            }

            override fun onReadReceiptReceived(channel: Channel?, from: User?, messageIds: MutableList<Long>) {
                onMessageStatusFlow_.tryEmit(MessageStatusChange(channel, from, DeliveryStatus.Read, messageIds))
            }

            override fun onClearedHistory(channel: Channel?) {
                onChannelEventChannel_.trySend(ChannelEventData(channel, ChannelEventEnum.ClearedHistory))
            }

            override fun onChannelUpdated(channel: Channel?) {
                onChannelEventChannel_.trySend(ChannelEventData(channel, ChannelEventEnum.Updated))
            }

            override fun onChannelCreated(channel: Channel?) {
                onChannelEventChannel_.trySend(ChannelEventData(channel, ChannelEventEnum.Created))
            }

            override fun onChannelDeleted(channelId: Long) {
                onChannelEventChannel_.trySend(ChannelEventData(null, ChannelEventEnum.Deleted, channelId))
            }

            override fun onChannelMuted(channel: Channel?) {
                onChannelEventChannel_.trySend(ChannelEventData(channel, ChannelEventEnum.Muted))
            }

            override fun onChannelUnMuted(channel: Channel?) {
                onChannelEventChannel_.trySend(ChannelEventData(channel, ChannelEventEnum.UnMuted))
            }
        })
    }
}