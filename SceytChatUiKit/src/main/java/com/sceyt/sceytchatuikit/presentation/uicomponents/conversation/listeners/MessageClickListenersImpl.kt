package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

open class MessageClickListenersImpl : MessageClickListeners.ClickListeners {
    private var defaultListeners: MessageClickListeners.ClickListeners? = null
    private var messageLongClickListener: MessageClickListeners.MessageLongClickListener? = null
    private var avatarClickListener: MessageClickListeners.AvatarClickListener? = null
    private var replayCountClickListener: MessageClickListeners.ReplayCountClickListener? = null
    private var addReactionClickListener: MessageClickListeners.AddReactionClickListener? = null
    private var reactionLongClickListener: MessageClickListeners.ReactionLongClickListener? = null
    private var reactionClickListener: MessageClickListeners.ReactionClickListener? = null
    private var attachmentClickListener: MessageClickListeners.AttachmentClickListener? = null
    private var attachmentLongClickListener: MessageClickListeners.AttachmentLongClickListener? = null
    private var linkClickListener: MessageClickListeners.LinkClickListener? = null


    internal constructor()

    constructor(view: MessagesListView) {
        defaultListeners = view
    }

    override fun onMessageLongClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onMessageLongClick(view, item)
        messageLongClickListener?.onMessageLongClick(view, item)
    }

    override fun onAvatarClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onAvatarClick(view, item)
        avatarClickListener?.onAvatarClick(view, item)
    }

    override fun onReplayCountClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onReplayCountClick(view, item)
        replayCountClickListener?.onReplayCountClick(view, item)
    }

    override fun onAddReactionClick(view: View, message: SceytMessage) {
        defaultListeners?.onAddReactionClick(view, message)
        addReactionClickListener?.onAddReactionClick(view, message)
    }

    override fun onReactionClick(view: View, item: ReactionItem.Reaction) {
        defaultListeners?.onReactionClick(view, item)
        reactionClickListener?.onReactionClick(view, item)
    }

    override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
        defaultListeners?.onReactionLongClick(view, item)
        reactionLongClickListener?.onReactionLongClick(view, item)
    }

    override fun onAttachmentClick(view: View, item: FileListItem) {
        defaultListeners?.onAttachmentClick(view, item)
        attachmentClickListener?.onAttachmentClick(view, item)
    }

    override fun onAttachmentLongClick(view: View, item: FileListItem) {
        defaultListeners?.onAttachmentLongClick(view, item)
        attachmentLongClickListener?.onAttachmentLongClick(view, item)
    }

    override fun onLinkClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onLinkClick(view, item)
        linkClickListener?.onLinkClick(view, item)
    }

    fun setListener(listener: MessageClickListeners) {
        when (listener) {
            is MessageClickListeners.ClickListeners -> {
                messageLongClickListener = listener
                avatarClickListener = listener
                replayCountClickListener = listener
                addReactionClickListener = listener
                reactionClickListener = listener
                reactionLongClickListener = listener
                attachmentClickListener = listener
                attachmentLongClickListener = listener
                linkClickListener = listener
            }
            is MessageClickListeners.MessageLongClickListener -> {
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
            is MessageClickListeners.ReactionClickListener -> {
                reactionClickListener = listener
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
            is MessageClickListeners.LinkClickListener -> {
                linkClickListener = listener
            }
        }
    }
}