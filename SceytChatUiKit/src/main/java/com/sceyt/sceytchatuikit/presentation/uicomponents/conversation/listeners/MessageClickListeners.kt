package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

sealed interface MessageClickListeners {

    fun interface MessageLongClickListener : MessageClickListeners {
        fun onMessageLongClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface AvatarClickListener : MessageClickListeners {
        fun onAvatarClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface ReplayCountClickListener : MessageClickListeners {
        fun onReplayCountClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface AddReactionClickListener : MessageClickListeners {
        fun onAddReactionClick(view: View, message: SceytMessage)
    }

    fun interface ReactionClickListener : MessageClickListeners {
        fun onReactionClick(view: View, item: ReactionItem.Reaction)
    }

    fun interface ReactionLongClickListener : MessageClickListeners {
        fun onReactionLongClick(view: View, item: ReactionItem.Reaction)
    }

    fun interface AttachmentClickListener : MessageClickListeners {
        fun onAttachmentClick(view: View, item: FileListItem)
    }

    fun interface AttachmentLongClickListener : MessageClickListeners {
        fun onAttachmentLongClick(view: View, item: FileListItem)
    }

    fun interface LinkClickListener : MessageClickListeners {
        fun onLinkClick(view: View, item: MessageListItem.MessageItem)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners :
            LinkClickListener,
            MessageLongClickListener,
            AvatarClickListener,
            ReplayCountClickListener,
            AddReactionClickListener,
            ReactionClickListener,
            ReactionLongClickListener,
            AttachmentClickListener,
            AttachmentLongClickListener
}