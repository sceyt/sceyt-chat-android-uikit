package com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click

import android.view.View
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem

open class MessageClickListenersImpl : MessageClickListeners.ClickListeners {
    private var defaultListeners: MessageClickListeners.ClickListeners? = null
    private var messageClickListener: MessageClickListeners.MessageClickListener? = null
    private var messageLongClickListener: MessageClickListeners.MessageLongClickListener? = null
    private var avatarClickListener: MessageClickListeners.AvatarClickListener? = null
    private var replyMessageContainerClickListener: MessageClickListeners.ReplyMessageContainerClickListener? = null
    private var replyCountClickListener: MessageClickListeners.ReplyCountClickListener? = null
    private var addReactionClickListener: MessageClickListeners.AddReactionClickListener? = null
    private var reactionClickListener: MessageClickListeners.ReactionClickListener? = null
    private var attachmentClickListener: MessageClickListeners.AttachmentClickListener? = null
    private var attachmentLongClickListener: MessageClickListeners.AttachmentLongClickListener? = null
    private var mentionUserClickListener: MessageClickListeners.MentionClickListener? = null
    private var attachmentLoaderClickListener: MessageClickListeners.AttachmentLoaderClickListener? = null
    private var linkClickListener: MessageClickListeners.LinkClickListener? = null
    private var linkDetailsClickListener: MessageClickListeners.LinkDetailsClickListener? = null
    private var scrollToDownClickListener: MessageClickListeners.ScrollToDownClickListener? = null
    private var scrollToUnreadMentionClickListener: MessageClickListeners.ScrollToUnreadMentionClickListener? = null
    private var multiSelectClickListener: MessageClickListeners.MultiSelectClickListener? = null
    private var pollOptionClickListener: MessageClickListeners.PollOptionClickListener? = null
    private var pollViewResultsClickListener: MessageClickListeners.PollViewResultsClickListener? = null
    private var pollVotersClickListener: MessageClickListeners.PollVotersClickListener? = null
    private var readMoreClickListener: MessageClickListeners.ReadMoreClickListener? = null

    constructor()

    internal constructor(view: MessagesListView) {
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

    override fun onReactionClick(view: View, item: ReactionItem.Reaction, message: SceytMessage) {
        defaultListeners?.onReactionClick(view, item, message)
        reactionClickListener?.onReactionClick(view, item, message)
    }

    override fun onAttachmentClick(view: View, item: FileListItem, message: SceytMessage) {
        defaultListeners?.onAttachmentClick(view, item, message)
        attachmentClickListener?.onAttachmentClick(view, item, message)
    }

    override fun onAttachmentLongClick(view: View, item: FileListItem, message: SceytMessage) {
        defaultListeners?.onAttachmentLongClick(view, item, message)
        attachmentLongClickListener?.onAttachmentLongClick(view, item, message)
    }

    override fun onMentionClick(view: View, userId: String) {
        defaultListeners?.onMentionClick(view, userId)
        mentionUserClickListener?.onMentionClick(view, userId)
    }

    override fun onAttachmentLoaderClick(view: View, item: FileListItem, message: SceytMessage) {
        defaultListeners?.onAttachmentLoaderClick(view, item, message)
        attachmentLoaderClickListener?.onAttachmentLoaderClick(view, item, message)
    }

    override fun onLinkClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onLinkClick(view, item)
        linkClickListener?.onLinkClick(view, item)
    }

    override fun onLinkDetailsClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onLinkDetailsClick(view, item)
        linkDetailsClickListener?.onLinkDetailsClick(view, item)
    }

    override fun onScrollToDownClick(view: View) {
        defaultListeners?.onScrollToDownClick(view)
        scrollToDownClickListener?.onScrollToDownClick(view)
    }

    override fun onScrollToUnreadMentionClick(view: View) {
        defaultListeners?.onScrollToUnreadMentionClick(view)
        scrollToUnreadMentionClickListener?.onScrollToUnreadMentionClick(view)
    }

    override fun onMultiSelectClick(view: View, message: SceytMessage) {
        defaultListeners?.onMultiSelectClick(view, message)
        multiSelectClickListener?.onMultiSelectClick(view, message)
    }

    override fun onPollOptionClick(view: View, item: MessageListItem.MessageItem, option: PollOption) {
        defaultListeners?.onPollOptionClick(view, item, option)
        pollOptionClickListener?.onPollOptionClick(view, item, option)
    }

    override fun onPollViewResultsClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onPollViewResultsClick(view, item)
        pollViewResultsClickListener?.onPollViewResultsClick(view, item)
    }

    override fun onPollVotersClick(view: View, item: MessageListItem.MessageItem, option: PollOption) {
        defaultListeners?.onPollVotersClick(view, item, option)
        pollVotersClickListener?.onPollVotersClick(view, item, option)
    }

    override fun onReadMoreClick(view: View, item: MessageListItem.MessageItem) {
        defaultListeners?.onReadMoreClick(view, item)
        readMoreClickListener?.onReadMoreClick(view, item)
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
                attachmentClickListener = listener
                attachmentLongClickListener = listener
                mentionUserClickListener = listener
                linkClickListener = listener
                linkDetailsClickListener = listener
                scrollToDownClickListener = listener
                scrollToUnreadMentionClickListener = listener
                attachmentLoaderClickListener = listener
                multiSelectClickListener = listener
                pollOptionClickListener = listener
                pollViewResultsClickListener = listener
                pollVotersClickListener = listener
                readMoreClickListener = listener
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

            is MessageClickListeners.LinkDetailsClickListener -> {
                linkDetailsClickListener = listener
            }

            is MessageClickListeners.ScrollToDownClickListener -> {
                scrollToDownClickListener = listener
            }

            is MessageClickListeners.ScrollToUnreadMentionClickListener -> {
                scrollToUnreadMentionClickListener = listener
            }

            is MessageClickListeners.MultiSelectClickListener -> {
                multiSelectClickListener = listener
            }

            is MessageClickListeners.PollOptionClickListener -> {
                pollOptionClickListener = listener
            }

            is MessageClickListeners.PollViewResultsClickListener -> {
                pollViewResultsClickListener = listener
            }

            is MessageClickListeners.PollVotersClickListener -> {
                pollVotersClickListener = listener
            }

            is MessageClickListeners.ReadMoreClickListener -> {
                readMoreClickListener = listener
            }
        }
    }

    internal fun withDefaultListeners(
            listener: MessageClickListeners.ClickListeners
    ): MessageClickListenersImpl {
        defaultListeners = listener
        return this
    }
}