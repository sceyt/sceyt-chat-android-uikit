package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

sealed interface MessageClickListeners {

    fun interface MessageClickLongClickListener : MessageClickListeners {
        fun onMessageLongClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface AvatarClickListener : MessageClickListeners {
        fun onAvatarClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface ReplayCountClickListener : MessageClickListeners {
        fun onReplayCountClick(view: View, item: MessageListItem.MessageItem)
    }

    fun interface AddReactionClickListener : MessageClickListeners {
        fun onAddReactionClick(view: View, item: MessageListItem.MessageItem)
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

    /** User this if you want to implement all callbacks */
    interface ClickListeners :
            MessageClickLongClickListener,
            AvatarClickListener,
            ReplayCountClickListener,
            AddReactionClickListener,
            ReactionLongClickListener,
            AttachmentClickListener,
            AttachmentLongClickListener
}