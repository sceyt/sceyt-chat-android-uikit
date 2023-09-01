package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.view.Menu
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView

class MessageActionBridge {
    private var messagesListView: MessagesListView? = null
    private var headerView: ConversationHeaderView? = null

    fun setMessagesListView(messagesListView: MessagesListView) {
        this.messagesListView = messagesListView
    }

    fun setHeaderView(headerView: ConversationHeaderView) {
        this.headerView = headerView
        headerView.setToolbarActionHiddenCallback {
            messagesListView?.getMessageCommandEventListener()?.invoke(MessageCommandEvent.OnCancelMultiselectEvent)
        }
    }

    fun showMessageActions(vararg selectedMessages: SceytMessage): Menu? {
        val messageActionListener = messagesListView?.messageActionsViewClickListeners
                ?: return null
        return headerView?.uiElementsListeners?.onShowMessageActionsMenu(*selectedMessages, menuResId = R.menu.sceyt_menu_message_actions) {
            val firstMessage = selectedMessages.getOrNull(0)
            when (it.itemId) {
                R.id.sceyt_edit_message -> firstMessage?.let { message ->
                    messageActionListener.onEditMessageClick(message)
                }

                R.id.sceyt_forward -> {
                    messageActionListener.onForwardMessageClick(*selectedMessages)
                }

                R.id.sceyt_reply -> firstMessage?.let { message ->
                    messageActionListener.onReplyMessageClick(message)
                }

                R.id.sceyt_reply_in_thread -> firstMessage?.let { message ->
                    messageActionListener.onReplyMessageInThreadClick(message)
                }

                R.id.sceyt_copy_message -> {
                    messageActionListener.onCopyMessagesClick(*selectedMessages)
                }

                R.id.sceyt_delete_message -> {
                    messageActionListener.onDeleteMessageClick(*selectedMessages, onlyForMe = false)
                }
            }
        }
    }

    fun hideMessageActions() {
        headerView?.uiElementsListeners?.onHideMessageActionsMenu()
    }
}