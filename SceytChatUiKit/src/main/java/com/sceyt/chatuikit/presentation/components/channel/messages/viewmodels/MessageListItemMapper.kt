package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.data.toFileListItem
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

internal class MessageListItemMapper {

    fun map(
        data: List<SceytMessage>?,
        hasNext: Boolean,
        hasPrev: Boolean,
        compareMessage: SceytMessage? = null,
        ignoreUnreadMessagesSeparator: Boolean = false,
        enableDateSeparator: Boolean,
        context: MessageListItemMappingContext,
    ): List<MessageListItem> {
        if (data.isNullOrEmpty()) return emptyList()

        val messageItems = arrayListOf<MessageListItem>()
        var unreadLineMessage: MessageListItem.UnreadMessagesSeparatorItem? = null
        data.forEachIndexed { index, message ->
            var prevMessage = compareMessage
            if (index > 0)
                prevMessage = data.getOrNull(index - 1)

            if (enableDateSeparator && shouldShowDate(message, prevMessage))
                messageItems.add(
                    MessageListItem.DateSeparatorItem(
                        createdAt = message.createdAt,
                        messageTid = message.tid,
                        messageId = message.id
                    )
                )

            var messageWithData = initMessageInfoData(
                sceytMessage = message,
                prevMessage = prevMessage,
                initNameAndAvatar = true,
                context = context
            )
            val isSelected = context.selectedMessageTids.contains(message.tid)
            val isExpanded = context.expandedMessageTids.contains(message.tid)

            if (context.channel.lastMessage?.incoming == true && context.pinnedLastReadMessageId != 0L
                && prevMessage?.id == context.pinnedLastReadMessageId && unreadLineMessage == null
            ) {
                messageWithData = messageWithData.copy(
                    shouldShowAvatarAndName = messageWithData.incoming && context.channel.isGroup
                            && context.showSenderAvatarAndName,
                    disabledShowAvatarAndName = !context.showSenderAvatarAndName,
                )
                if (!ignoreUnreadMessagesSeparator)
                    messageItems.add(
                        MessageListItem.UnreadMessagesSeparatorItem(
                            createdAt = message.createdAt,
                            msgId = context.pinnedLastReadMessageId
                        ).also {
                            unreadLineMessage = it
                        })
            }

            messageItems.add(
                MessageItem(
                    messageWithData.copy(
                        isSelected = isSelected,
                        isBodyExpanded = isExpanded
                    )
                )
            )
        }

        if (hasNext)
            messageItems.add(MessageListItem.LoadingNextItem)

        if (hasPrev)
            messageItems.add(0, MessageListItem.LoadingPrevItem)

        return messageItems
    }

    fun initMessageInfoData(
        sceytMessage: SceytMessage,
        prevMessage: SceytMessage? = null,
        initNameAndAvatar: Boolean = false,
        context: MessageListItemMappingContext,
    ): SceytMessage {
        return sceytMessage.copy(
            isGroup = context.channel.isGroup,
            files = sceytMessage.attachments?.map { it.toFileListItem() },
            shouldShowAvatarAndName = if (initNameAndAvatar && context.showSenderAvatarAndName)
                shouldShowAvatarAndName(sceytMessage, prevMessage, context.channel)
            else sceytMessage.shouldShowAvatarAndName,
            disabledShowAvatarAndName = !context.showSenderAvatarAndName,
            messageReactions = initReactionsItems(sceytMessage, context.myIdProvider),
        )
    }

    private fun initReactionsItems(
        message: SceytMessage,
        myIdProvider: () -> String?,
    ): List<ReactionItem.Reaction>? {
        val pendingReactions = message.pendingReactions
        val reactionItems = message.reactionTotals?.map {
            val myId = myIdProvider()
            ReactionItem.Reaction(
                SceytReactionTotal(
                    key = it.key, score = it.score.toInt(),
                    containsSelf = message.userReactions?.find { reaction ->
                        reaction.key == it.key && reaction.user?.id == myId
                    } != null), message.tid, false)
        }?.toArrayList()

        if (!pendingReactions.isNullOrEmpty() && reactionItems != null) {
            pendingReactions.forEach { pendingReaction ->
                reactionItems.findIndexed { it.reaction.key == pendingReaction.key }
                    ?.let { (index, item) ->
                        val reaction = item.reaction
                        if (pendingReaction.isAdd) {
                            reactionItems[index] = item.copy(
                                reaction = reaction.copy(
                                    score = reaction.score + pendingReaction.score,
                                    containsSelf = true
                                ),
                                isPending = true
                            )
                        } else {
                            val score = reaction.score - pendingReaction.score
                            if (score <= 0)
                                reactionItems.remove(item)
                            else {
                                reactionItems[index] = item.copy(
                                    reaction = reaction.copy(
                                        score = reaction.score - pendingReaction.score,
                                        containsSelf = false
                                    ),
                                    isPending = false
                                )
                            }
                        }
                    } ?: run {
                    if (pendingReaction.isAdd)
                        reactionItems.add(
                            ReactionItem.Reaction(
                                reaction = SceytReactionTotal(
                                    pendingReaction.key,
                                    pendingReaction.score,
                                    true
                                ),
                                messageTid = message.tid,
                                isPending = true
                            )
                        )
                }
            }
        }
        return reactionItems?.sortedBy { it.reaction.key }
    }

    private fun shouldShowDate(sceytMessage: SceytMessage, prevMessage: SceytMessage?): Boolean {
        return if (prevMessage == null)
            true
        else !DateTimeUtil.isSameDay(sceytMessage.createdAt, prevMessage.createdAt)
    }

    private fun shouldShowAvatarAndName(
        sceytMessage: SceytMessage,
        prevMessage: SceytMessage?,
        channel: SceytChannel,
    ): Boolean {
        if (!sceytMessage.incoming) return false
        return if (prevMessage == null)
            channel.isGroup
        else {
            val sameSender = prevMessage.user?.id == sceytMessage.user?.id
            channel.isGroup && (!sameSender || shouldShowDate(sceytMessage, prevMessage)
                    || prevMessage.type == SceytMessageType.System.value)
        }
    }
}

internal data class MessageListItemMappingContext(
    val channel: SceytChannel,
    val myIdProvider: () -> String?,
    val pinnedLastReadMessageId: Long,
    val showSenderAvatarAndName: Boolean,
    val selectedMessageTids: Set<MessageTid>,
    val expandedMessageTids: Set<MessageTid>,
)
