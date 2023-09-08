package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.ScrollToDownView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

open class MessageClickListenersImpl : MessageClickListeners.ClickListeners {
    private var defaultListeners: MessageClickListeners.ClickListeners? = null
    private var messageClickListener: MessageClickListeners.MessageClickListener? = null
    private var messageLongClickListener: MessageClickListeners.MessageLongClickListener? = null
    private var avatarClickListener: MessageClickListeners.AvatarClickListener? = null
    private var replyMessageContainerClickListener: MessageClickListeners.ReplyMessageContainerClickListener? = null
    private var replyCountClickListener: MessageClickListeners.ReplyCountClickListener? = null
    private var addReactionClickListener: MessageClickListeners.AddReactionClickListener? = null
    private var reactionLongClickListener: MessageClickListeners.ReactionLongClickListener? = null
    private var reactionClickListener: MessageClickListeners.ReactionClickListener? = null
    private var attachmentClickListener: MessageClickListeners.AttachmentClickListener? = null
    private var attachmentLongClickListener: MessageClickListeners.AttachmentLongClickListener? = null
    private var mentionUserClickListener: MessageClickListeners.MentionClickListener? = null
    private var attachmentLoaderClickListener: MessageClickListeners.AttachmentLoaderClickListener? = null
    private var linkClickListener: MessageClickListeners.LinkClickListener? = null
    private var scrollToDownClickListener: MessageClickListeners.ScrollToDownClickListener? = null
    private var multiSelectClickListener: MessageClickListeners.MultiSelectClickListener? = null


    internal constructor()

    constructor(view: MessagesListView) {
        defaultListeners = view
    }

    override fun onMessageClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onMessageClick(view, item)
        messageClickListener?.onMessageClick(view, item)
    }

    override fun onMessageLongClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onMessageLongClick(view, item)
        messageLongClickListener?.onMessageLongClick(view, item)
    }

    override fun onAvatarClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onAvatarClick(view, item)
        avatarClickListener?.onAvatarClick(view, item)
    }

    override fun onReplyMessageContainerClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onReplyMessageContainerClick(view, item)
        replyMessageContainerClickListener?.onReplyMessageContainerClick(view, item)
    }

    override fun onReplyCountClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onReplyCountClick(view, item)
        replyCountClickListener?.onReplyCountClick(view, item)
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

    override fun onMentionClick(view: View, userId: String) {
        defaultListeners?.onMentionClick(view, userId)
        mentionUserClickListener?.onMentionClick(view, userId)
    }

    override fun onAttachmentLoaderClick(view: View, item: FileListItem) {
        defaultListeners?.onAttachmentLoaderClick(view, item)
        attachmentLoaderClickListener?.onAttachmentLoaderClick(view, item)
    }

    override fun onLinkClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onLinkClick(view, item)
        linkClickListener?.onLinkClick(view, item)
    }

    override fun onScrollToDownClick(view: ScrollToDownView) {
        defaultListeners?.onScrollToDownClick(view)
        scrollToDownClickListener?.onScrollToDownClick(view)
    }

    override fun onMultiSelectClick(view: View, message: SceytMessage) {
        defaultListeners?.onMultiSelectClick(view, message)
        multiSelectClickListener?.onMultiSelectClick(view, message)
    }

    fun setListener(listener: MessageClickListeners) {
        when (listener) {
            is MessageClickListeners.ClickListeners -> {
                messageClickListener = listener
                messageLongClickListener = listener
                avatarClickListener = listener
                replyMessageContainerClickListener = listener
                replyCountClickListener = listener
                addReactionClickListener = listener
                reactionClickListener = listener
                reactionLongClickListener = listener
                attachmentClickListener = listener
                attachmentLongClickListener = listener
                mentionUserClickListener = listener
                linkClickListener = listener
                scrollToDownClickListener = listener
                attachmentLoaderClickListener = listener
                multiSelectClickListener = listener
            }

            is MessageClickListeners.MessageClickListener -> {
                messageClickListener = listener
            }

            is MessageClickListeners.MessageLongClickListener -> {
                messageLongClickListener = listener
            }

            is MessageClickListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }

            is MessageClickListeners.ReplyMessageContainerClickListener -> {
                replyMessageContainerClickListener = listener
            }

            is MessageClickListeners.ReplyCountClickListener -> {
                replyCountClickListener = listener
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

            is MessageClickListeners.MentionClickListener -> {
                mentionUserClickListener = listener
            }

            is MessageClickListeners.AttachmentLoaderClickListener -> {
                attachmentLoaderClickListener = listener
            }

            is MessageClickListeners.LinkClickListener -> {
                linkClickListener = listener
            }

            is MessageClickListeners.ScrollToDownClickListener -> {
                scrollToDownClickListener = listener
            }

            is MessageClickListeners.MultiSelectClickListener -> {
                multiSelectClickListener = listener
            }
        }
    }
}