package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chat.models.Types
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.data.models.messages.MessageId
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the unread-mention tracking that used to live inside [MessageListViewModel]:
 * loading unread mention ids, keeping the channel mention counter in sync, and
 * picking the next mention to scroll to.
 *
 * All state mutation goes through a [MutableStateFlow] so concurrent updates from
 * different coroutines/dispatchers stay atomic.
 */
internal class UnreadMentionsController(
    private val scope: CoroutineScope,
    private val messageInteractor: MessageInteractor,
    private val channelInteractor: ChannelInteractor,
    private val currentChannel: () -> SceytChannel,
    private val conversationId: () -> Long,
    private val updateChannel: (SceytChannel.() -> SceytChannel) -> Unit,
    private val onScrollToMention: (Long) -> Unit,
    private val currentUserId: () -> String?,
) {
    private val state = MutableStateFlow(UnreadMentionState())

    fun onInit() {
        if (currentChannel().newMentionCount > 0) {
            load(0)
        }
    }

    fun onNewMessage(message: SceytMessage) {
        if (!message.incoming || message.displayCount.toInt() == 0 || message.disableMentionsCount)
            return

        if (message.mentionedUsers?.any { it.id == currentUserId() } == true) {
            val prev = state.getAndUpdate { current ->
                current.copy(messageIds = current.messageIds.plus(message.id))
            }
            // Only bump the channel counter when this id was not already tracked,
            // otherwise re-processing the same message drifts the count upward.
            if (!prev.messageIds.contains(message.id)) {
                updateChannel {
                    copy(newMentionCount = newMentionCount + 1)
                }
            }
        }
    }

    fun onMessageUpdated(message: SceytMessage) {
        if (!message.incoming || message.displayCount.toInt() == 0) return
        val mentionsMe = message.mentionedUsers.orEmpty()
            .any { it.id == currentUserId() }

        val prev = state.getAndUpdate { current ->
            val contains = current.messageIds.contains(message.id)
            when {
                contains && !mentionsMe -> current.copy(messageIds = current.messageIds.minus(message.id))
                !contains && mentionsMe -> current.copy(messageIds = current.messageIds.plus(message.id))
                else -> current
            }
        }

        val changed = prev.messageIds.contains(message.id) !=
                state.value.messageIds.contains(message.id)
        if (changed) {
            scope.launch {
                // Get channel from server to update new mentions count
                channelInteractor.getChannelFromServer(currentChannel().id)
            }
        }
    }

    fun removeReadMentions(ids: Collection<Long>) {
        if (state.value.messageIds.isEmpty()) return
        state.update { it.copy(messageIds = it.messageIds.minus(ids.toSet())) }
    }

    fun prepareToScrollToNext() {
        getNext()?.let { onScrollToMention(it.messageId) }
    }

    private fun getNext(): MessageId? {
        val current = state.value
        if (current.messageIds.isEmpty()) {
            if (currentChannel().newMentionCount > 0) {
                load(0, scrollTo = true)
            }
            return null
        }

        val element = current.messageIds.first()
        val remaining = current.messageIds.minus(element)
        state.update { it.copy(messageIds = remaining) }

        if (current.hasMore && remaining.size < 5) {
            load((remaining.lastOrNull() ?: element))
        }
        return MessageId(element)
    }

    private fun load(messageId: Long, scrollTo: Boolean = false) {
        if (state.value.isLoadingMore) return
        state.update { it.copy(isLoadingMore = true) }
        scope.launch {
            messageInteractor.getUnreadMentions(
                conversationId = conversationId(),
                direction = Types.Direction.DirectionNext,
                messageId = messageId
            ).fold(
                onSuccess = { response ->
                    val unreadMentions = if (messageId == 0L)
                        response.data.toSet()
                    else state.value.messageIds.plus(response.data)

                    state.update { current ->
                        current.copy(
                            messageIds = unreadMentions,
                            hasMore = response.hasNext,
                            isLoadingMore = false
                        )
                    }
                    if (currentChannel().newMentionCount > 0 && unreadMentions.isEmpty()) {
                        updateChannel {
                            copy(newMentionCount = 0)
                        }
                    }

                    if (scrollTo)
                        prepareToScrollToNext()
                },
                onError = {
                    state.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }
}