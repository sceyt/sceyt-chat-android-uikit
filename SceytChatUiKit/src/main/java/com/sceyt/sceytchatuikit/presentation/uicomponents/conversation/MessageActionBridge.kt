package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.view.Menu
import android.widget.PopupWindow
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView

class MessageActionBridge {
    private var messagesListView: MessagesListView? = null
    private var headerView: ConversationHeaderView? = null

    fun setMessagesListView(messagesListView: MessagesListView) {
        this.messagesListView = messagesListView
    }

    fun setHeaderView(headerView: ConversationHeaderView) {
        this.headerView = headerView
    }

    fun showMessageActions(message: SceytMessage, popupWindow: PopupWindow?): Menu? {
        val messageActionListener = messagesListView?.messageActionsViewClickListeners
                ?: return null
        return headerView?.uiElementsListeners?.onShowMessageActionsMenu(message, R.menu.sceyt_menu_message_actions, popupWindow) {
            when (it.itemId) {
                R.id.sceyt_edit_message -> messageActionListener.onEditMessageClick(message)
                R.id.sceyt_forward -> messageActionListener.onForwardMessageClick(message)
                R.id.sceyt_reply -> messageActionListener.onReplyMessageClick(message)
                R.id.sceyt_reply_in_thread -> messageActionListener.onReplyMessageInThreadClick(message)
                R.id.sceyt_copy_message -> messageActionListener.onCopyMessageClick(message)
                R.id.sceyt_delete_message -> messageActionListener.onDeleteMessageClick(message, false)
            }
        }
    }
}