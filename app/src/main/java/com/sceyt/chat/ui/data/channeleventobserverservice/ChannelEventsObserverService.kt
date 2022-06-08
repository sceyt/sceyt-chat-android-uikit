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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ChannelEventsObserverService {
    private val onMessageFlow_: MutableSharedFlow<Pair<Channel, Message>> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageFlow = onMessageFlow_.asSharedFlow()

    private val onMessageStatusFlow_: MutableSharedFlow<MessageStatusChange> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageStatusFlow = onMessageStatusFlow_.asSharedFlow()

    private val onMessageEditedOrDeletedFlow_: MutableSharedFlow<Message?> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageEditedOrDeletedFlow = onMessageEditedOrDeletedFlow_.asSharedFlow()

    private val onMessageReactionUpdatedFlow_: MutableSharedFlow<Message?> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageReactionUpdatedFlow = onMessageReactionUpdatedFlow_.asSharedFlow()

    private val onChannelEventFlow_ = MutableSharedFlow<ChannelEventData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelEventFlow: SharedFlow<ChannelEventData> = onChannelEventFlow_.asSharedFlow()

    init {
        ChatClient.getClient().addMessageListener(TAG, object : MessageListener {

            override fun onMessage(channel: Channel, message: Message) {
                onMessageFlow_.tryEmit(Pair(channel, message))

                ClientWrapper.markMessagesAsReceived(channel.id, longArrayOf(message.id)) { _, _ ->
                }
            }

            override fun onMessageDeleted(message: Message?) {
                onMessageEditedOrDeletedFlow_.tryEmit(message)
            }

            override fun onMessageEdited(message: Message?) {
                onMessageEditedOrDeletedFlow_.tryEmit(message)
            }

            override fun onReactionAdded(message: Message?, reaction: Reaction?) {
                onMessageReactionUpdatedFlow_.tryEmit(message)
            }

            override fun onReactionDeleted(message: Message?, reaction: Reaction?) {
                onMessageReactionUpdatedFlow_.tryEmit(message)
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
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.ClearedHistory))
            }

            override fun onChannelUpdated(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Updated))
            }

            override fun onChannelCreated(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Created))
            }

            override fun onChannelDeleted(channelId: Long) {
                val data = ChannelEventData(null, ChannelEventEnum.Deleted, channelId)
                onChannelEventFlow_.tryEmit(data)
            }

            override fun onChannelMuted(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Muted))
            }

            override fun onChannelUnMuted(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.UnMuted))
            }
        })
    }
}