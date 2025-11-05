package com.sceyt.chatuikit.data.managers.message

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.poll.PollVote
import com.sceyt.chat.sceyt_listeners.MessageListener
import com.sceyt.chatuikit.data.managers.message.event.PollUpdateEvent
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventData
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventEnum
import com.sceyt.chatuikit.data.managers.message.handler.MessageEventHandler.AllEventManagers
import com.sceyt.chatuikit.data.managers.message.handler.MessageEventHandlerImpl
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.mappers.toVote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

object MessageEventManager : AllEventManagers {
    private var eventManager: AllEventManagers = MessageEventHandlerImpl(this)

    private val onMessageFlow_: MutableSharedFlow<Pair<SceytChannel, SceytMessage>> =
        MutableSharedFlow(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val onMessageFlow = onMessageFlow_.asSharedFlow()

    private val onDirectMessageFlow_: MutableSharedFlow<SceytMessage> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onDirectMessageFlow = onDirectMessageFlow_.asSharedFlow()

    private val onMessageReactionUpdatedFlow_: MutableSharedFlow<ReactionUpdateEventData> =
        MutableSharedFlow(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val onMessageReactionUpdatedFlow = onMessageReactionUpdatedFlow_.asSharedFlow()


    private val onMessageEditedOrDeletedFlow_: MutableSharedFlow<SceytMessage> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onMessageEditedOrDeletedFlow = onMessageEditedOrDeletedFlow_.asSharedFlow()


    private val onPollUpdatedFlow_: MutableSharedFlow<PollUpdateEvent> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onPollUpdatedFlow = onPollUpdatedFlow_.asSharedFlow()


    private val onOutGoingMessageFlow_: MutableSharedFlow<SceytMessage> = MutableSharedFlow(
        extraBufferCapacity = 50,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onOutgoingMessageFlow = onOutGoingMessageFlow_.asSharedFlow()


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
                eventManager.onReactionDeleted(
                    message = message.toSceytUiMessage(),
                    reaction = reaction.toSceytReaction()
                )
            }

            override fun onVoteChanged(
                message: Message?,
                addedVotes: List<PollVote>?,
                removedVotes: List<PollVote>?
            ) {
                if (message == null) return
                eventManager.onVoteChanged(
                    message = message.toSceytUiMessage(),
                    addedVotes = addedVotes?.map { it.toVote() }.orEmpty(),
                    removedVoted = removedVotes?.map { it.toVote() }.orEmpty()
                )
            }

            override fun onVoteRetracted(message: Message?, list: List<PollVote>?) {
                message ?: return
                eventManager.onVoteRetracted(
                    message = message.toSceytUiMessage(),
                    retractedVotes = list?.map { it.toVote() }.orEmpty()
                )
            }

            override fun onPollClosed(message: Message?) {
                message ?: return
                eventManager.onPollClosed(message.toSceytUiMessage())
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
        onMessageReactionUpdatedFlow_.tryEmit(
            ReactionUpdateEventData(
                message = message,
                reaction = reaction,
                eventType = ReactionUpdateEventEnum.Add
            )
        )
    }

    override fun onReactionDeleted(message: SceytMessage, reaction: SceytReaction) {
        onMessageReactionUpdatedFlow_.tryEmit(
            ReactionUpdateEventData(
                message = message,
                reaction = reaction,
                eventType = ReactionUpdateEventEnum.Remove
            )
        )
    }

    override fun onVoteChanged(
        message: SceytMessage,
        addedVotes: List<Vote>,
        removedVoted: List<Vote>
    ) {
        onPollUpdatedFlow_.tryEmit(
            value = PollUpdateEvent.VoteChanged(
                message = message,
                addedVotes = addedVotes,
                removedVotes = removedVoted,
            )
        )
    }

    override fun onVoteRetracted(message: SceytMessage, retractedVotes: List<Vote>) {
        onPollUpdatedFlow_.tryEmit(
            value = PollUpdateEvent.VoteRetracted(
                message = message,
                retractedVotes = retractedVotes,
            )
        )
    }

    override fun onPollClosed(message: SceytMessage) {
        onPollUpdatedFlow_.tryEmit(PollUpdateEvent.PollClosed(message = message))
    }

    @Suppress("unused")
    fun setCustomListener(listener: AllEventManagers) {
        eventManager = listener
        (eventManager as? MessageEventHandlerImpl)?.setDefaultListeners(this)
    }

    suspend fun emitOutgoingMessage(sceytMessage: SceytMessage) {
        withContext(Dispatchers.Main.immediate) {
            onOutGoingMessageFlow_.emit(sceytMessage)
        }
    }
}