package com.sceyt.sceytchatuikit.data.messageeventobserver

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.sceyt_listeners.MessageListener
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytReaction
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MessageEventsObserver : MessageEventManger.AllEventManagers {
    private var eventManager = MessageEventManagerImpl(this)

    private val onMessageFlow_: MutableSharedFlow<Pair<SceytChannel, SceytMessage>> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageFlow = onMessageFlow_.asSharedFlow()

    private val onDirectMessageFlow_: MutableSharedFlow<SceytMessage> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onDirectMessageFlow = onDirectMessageFlow_.asSharedFlow()

    private val onMessageReactionUpdatedFlow_: MutableSharedFlow<ReactionUpdateEventData> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageReactionUpdatedFlow = onMessageReactionUpdatedFlow_.asSharedFlow()


    private val onMessageEditedOrDeletedFlow_: MutableSharedFlow<SceytMessage> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageEditedOrDeletedFlow = onMessageEditedOrDeletedFlow_.asSharedFlow()


    private val onOutGoingMessageFlow_: MutableSharedFlow<SceytMessage> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onOutgoingMessageFlow = onOutGoingMessageFlow_.asSharedFlow()


    private val onOutGoingMessageStatusFlow_: MutableSharedFlow<Pair<Long, SceytMessage>> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onOutGoingMessageStatusFlow = onOutGoingMessageStatusFlow_.asSharedFlow()


    init {
        ChatClient.getClient().addMessageListener(TAG, object : MessageListener {

            override fun onMessage(channel: Channel, message: Message) {
                eventManager.onMessage(channel.toSceytUiChannel(), message.toSceytUiMessage())
            }

            override fun onDirectMessage(message: Message) {
                eventManager.onDirectMessage(message.toSceytUiMessage())
            }

            override fun onMessageDeleted(message: Message?) {
                message ?: return
                eventManager.onMessageDeleted(message.toSceytUiMessage())
            }

            override fun onMessageEdited(message: Message?) {
                message ?: return
                eventManager.onMessageEdited(message.toSceytUiMessage())
            }

            override fun onReactionAdded(message: Message?, reaction: Reaction?) {
                if (message == null || reaction == null) return
                eventManager.onReactionAdded(message.toSceytUiMessage(), reaction.toSceytReaction())
            }

            override fun onReactionDeleted(message: Message?, reaction: Reaction?) {
                if (message == null || reaction == null) return
                eventManager.onReactionDeleted(message.toSceytUiMessage(), reaction.toSceytReaction())
            }

            override fun onMessageComposing(p0: String?, p1: String?) {
            }

            override fun onMessagePaused(p0: String?, p1: String?) {
            }
        })
    }


    override fun onMessage(channel: SceytChannel, message: SceytMessage) {
        onMessageFlow_.tryEmit(Pair(channel, message))
    }

    override fun onDirectMessage(message: SceytMessage) {
        onDirectMessageFlow_.tryEmit(message)
    }

    override fun onMessageDeleted(message: SceytMessage) {
        onMessageEditedOrDeletedFlow_.tryEmit(message)
    }

    override fun onMessageEdited(message: SceytMessage) {
        onMessageEditedOrDeletedFlow_.tryEmit(message)
    }

    override fun onReactionAdded(message: SceytMessage, reaction: SceytReaction) {
        onMessageReactionUpdatedFlow_.tryEmit(ReactionUpdateEventData(message, reaction, ReactionUpdateEventEnum.Add))
    }

    override fun onReactionDeleted(message: SceytMessage, reaction: SceytReaction) {
        onMessageReactionUpdatedFlow_.tryEmit(ReactionUpdateEventData(message, reaction, ReactionUpdateEventEnum.Remove))
    }

    fun setCustomListener(listener: MessageEventManagerImpl) {
        eventManager = listener
        eventManager.setDefaultListeners(this)
    }

    fun emitOutgoingMessage(sceytMessage: SceytMessage) {
        onOutGoingMessageFlow_.tryEmit(sceytMessage)
    }

    fun emitOutgoingMessageSent(channelId: Long, message: SceytMessage) {
        onOutGoingMessageStatusFlow_.tryEmit(Pair(channelId, message))
    }
}