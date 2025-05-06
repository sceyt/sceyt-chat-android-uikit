package com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click

import android.view.View
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.components.channel.messages.components.ScrollToDownView

sealed interface MessageClickListeners {

    fun interface MessageClickListener : MessageClickListeners {
        fun onMessageClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface MessageLongClickListener : MessageClickListeners {
        fun onMessageLongClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface AvatarClickListener : MessageClickListeners {
        fun onAvatarClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface ReplyMessageContainerClickListener : MessageClickListeners {
        fun onReplyMessageContainerClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface ReplyCountClickListener : MessageClickListeners {
        fun onReplyCountClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface AddReactionClickListener : MessageClickListeners {
        fun onAddReactionClick(view: View, message: SceytMessage)
    }

    fun interface ReactionClickListener : MessageClickListeners {
        fun onReactionClick(view: View, item: ReactionItem.Reaction, message: SceytMessage)
    }

    fun interface AttachmentClickListener : MessageClickListeners {
        fun onAttachmentClick(view: View, item: FileListItem, message: SceytMessage)
    }

    fun interface AttachmentLongClickListener : MessageClickListeners {
        fun onAttachmentLongClick(view: View, item: FileListItem, message: SceytMessage)
    }

    fun interface MentionClickListener : MessageClickListeners {
        fun onMentionClick(view: View, userId: String)
    }

    fun interface AttachmentLoaderClickListener : MessageClickListeners {
        fun onAttachmentLoaderClick(view: View, item: FileListItem, message: SceytMessage)
    }

    fun interface LinkClickListener : MessageClickListeners {
        fun onLinkClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface LinkDetailsClickListener : MessageClickListeners {
        fun onLinkDetailsClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface ScrollToDownClickListener : MessageClickListeners {
        fun onScrollToDownClick(view: ScrollToDownView)
    }

    fun interface MultiSelectClickListener : MessageClickListeners {
        fun onMultiSelectClick(view: View, message: SceytMessage)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners :
            MessageClickListener,
            MessageLongClickListener,
            LinkClickListener,
            LinkDetailsClickListener,
            AvatarClickListener,
            ReplyMessageContainerClickListener,
            ReplyCountClickListener,
            AddReactionClickListener,
            ReactionClickListener,
            AttachmentClickListener,
            AttachmentLongClickListener,
            MentionClickListener,
            ScrollToDownClickListener,
            AttachmentLoaderClickListener,
            MultiSelectClickListener
}

internal fun MessageClickListeners.setListener(listener: MessageClickListeners) {
    (this as? MessageClickListenersImpl)?.setListener(listener)
}