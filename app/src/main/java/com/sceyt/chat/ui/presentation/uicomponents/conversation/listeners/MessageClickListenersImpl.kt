package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

class MessageClickListenersImpl : MessageClickListeners.ClickListeners {
    private var messageLongClickListener: MessageClickListeners.MessageClickLongClickListener? = null
    private var avatarClickListener: MessageClickListeners.AvatarClickListener? = null
    private var replayCountClickListener: MessageClickListeners.ReplayCountClickListener? = null
    private var addReactionClickListener: MessageClickListeners.AddReactionClickListener? = null
    private var reactionLongClickListener: MessageClickListeners.ReactionLongClickListener? = null
    private var attachmentClickListener: MessageClickListeners.AttachmentClickListener? = null
    private var attachmentLongClickListener: MessageClickListeners.AttachmentLongClickListener? = null


    override fun onMessageLongClick(view: View, item: MessageListItem.MessageItem) {
        messageLongClickListener?.onMessageLongClick(view, item)
    }

    override fun onAvatarClick(view: View, item: MessageListItem.MessageItem) {
        avatarClickListener?.onAvatarClick(view, item)
    }

    override fun onReplayCountClick(view: View, item: MessageListItem.MessageItem) {
        replayCountClickListener?.onReplayCountClick(view, item)
    }

    override fun onAddReactionClick(view: View, item: MessageListItem.MessageItem) {
        addReactionClickListener?.onAddReactionClick(view, item)
    }

    override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
        reactionLongClickListener?.onReactionLongClick(view, item)
    }

    override fun onAttachmentClick(view: View, item: FileListItem) {
        attachmentClickListener?.onAttachmentClick(view, item)
    }

    override fun onAttachmentLongClick(view: View, item: FileListItem) {
        attachmentLongClickListener?.onAttachmentLongClick(view, item)
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
                attachmentLongClickListener = listener
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
            is MessageClickListeners.AttachmentLongClickListener -> {
                attachmentLongClickListener = listener
            }
        }
    }
}