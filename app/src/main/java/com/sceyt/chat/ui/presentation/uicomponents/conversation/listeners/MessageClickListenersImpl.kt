package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

class MessageClickListenersImpl : MessageClickListeners.ClickListeners {
    private var messageLongClickListener: MessageClickListeners.MessageClickLongClickListener? = null
    private var avatarClickListener: MessageClickListeners.AvatarClickListener? = null
    private var replayCountClickListener: MessageClickListeners.ReplayCountClickListener? = null
    private var addReactionClickListener: MessageClickListeners.AddReactionClickListener? = null
    private var reactionLongClickListener: MessageClickListeners.ReactionLongClickListener? = null
    private var attachmentClickListener: MessageClickListeners.AttachmentClickListener? = null


    override fun onMessageLongClick(item: MessageListItem.MessageItem) {
        messageLongClickListener?.onMessageLongClick(item)
    }

    override fun onAvatarClick(item: MessageListItem.MessageItem) {
        avatarClickListener?.onAvatarClick(item)
    }

    override fun onReplayCountClick(item: MessageListItem.MessageItem) {
        replayCountClickListener?.onReplayCountClick(item)
    }

    override fun onAddReactionClick(item: MessageListItem.MessageItem, position: Int) {
        addReactionClickListener?.onAddReactionClick(item, position)
    }

    override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
        reactionLongClickListener?.onReactionLongClick(view, item)
    }

    override fun onAttachmentClick(item: MessageListItem.MessageItem) {
        attachmentClickListener?.onAttachmentClick(item)
    }

    fun setListener(listener: MessageClickListeners) {
        when (listener) {
            is MessageClickListeners.ClickListeners -> {
                messageLongClickListener = listener
                avatarClickListener = listener
                replayCountClickListener = listener
                addReactionClickListener = listener
                reactionLongClickListener = listener
                attachmentClickListener = listener
            }
            is MessageClickListeners.MessageClickLongClickListener -> {
                messageLongClickListener = listener
            }
            is MessageClickListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }
            is MessageClickListeners.ReplayCountClickListener -> {
                replayCountClickListener = listener
            }
            is MessageClickListeners.AddReactionClickListener -> {
                addReactionClickListener = listener
            }
            is MessageClickListeners.ReactionLongClickListener -> {
                reactionLongClickListener = listener
            }
            is MessageClickListeners.AttachmentClickListener -> {
                attachmentClickListener = listener
            }
        }
    }
}