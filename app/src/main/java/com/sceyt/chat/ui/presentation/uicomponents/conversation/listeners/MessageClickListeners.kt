package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

sealed interface MessageClickListeners {

    fun interface MessageClickLongClickListener : MessageClickListeners {
        fun onMessageLongClick(item: MessageListItem.MessageItem)
    }

    fun interface AvatarClickListener : MessageClickListeners {
        fun onAvatarClick(item: MessageListItem.MessageItem)
    }

    fun interface ReplayCountClickListener : MessageClickListeners {
        fun onReplayCountClick(item: MessageListItem.MessageItem)
    }

    fun interface AddReactionClickListener : MessageClickListeners {
        fun onAddReactionClick(item: MessageListItem.MessageItem, position: Int)
    }

    fun interface ReactionLongClickListener : MessageClickListeners {
        fun onReactionLongClick(view: View, item: ReactionItem.Reaction)
    }

    fun interface AttachmentClickListener : MessageClickListeners {
        fun onAttachmentClick(item: MessageListItem.MessageItem)
    }

    /** User this if you want to implement all callbacks */
    interface ClickListeners :
            MessageClickLongClickListener,
            AvatarClickListener,
            ReplayCountClickListener,
            AddReactionClickListener,
            ReactionLongClickListener,
            AttachmentClickListener
}