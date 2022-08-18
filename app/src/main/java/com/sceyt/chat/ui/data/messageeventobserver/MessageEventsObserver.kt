package com.sceyt.chat.ui.data.messageeventobserver

import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.sceyt_listeners.MessageListener
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.TAG
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MessageEventsObserver {
    private val onMessageFlow_: MutableSharedFlow<Pair<Channel, Message>> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageFlow = onMessageFlow_.asSharedFlow()


    private val onMessageReactionUpdatedFlow_: MutableSharedFlow<Message?> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageReactionUpdatedFlow = onMessageReactionUpdatedFlow_.asSharedFlow()


    private val onMessageEditedOrDeletedFlow_: MutableSharedFlow<Message?> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageEditedOrDeletedFlow = onMessageEditedOrDeletedFlow_.asSharedFlow()


    private val onOutGoingMessageFlow_: MutableSharedFlow<SceytMessage> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onOutgoingMessageFlow = onOutGoingMessageFlow_.asSharedFlow()


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
    }


    fun emitOutgoingMessage(sceytMessage: SceytMessage) {
        onOutGoingMessageFlow_.tryEmit(sceytMessage)
    }

    fun emitMessageEditedOrDeletedByMe(message: Message) {
        onMessageEditedOrDeletedFlow_.tryEmit(message)
    }
}