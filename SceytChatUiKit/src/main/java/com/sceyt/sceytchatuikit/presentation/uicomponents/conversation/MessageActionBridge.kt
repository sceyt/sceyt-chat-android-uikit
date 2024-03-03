package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.view.Menu
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView

class MessageActionBridge {
    private var messagesListView: MessagesListView? = null
    private var headerView: ConversationHeaderView? = null
    private var inputView: MessageInputView? = null

    fun setMessagesListView(messagesListView: MessagesListView) {
        this.messagesListView = messagesListView
    }

    fun setHeaderView(headerView: ConversationHeaderView) {
        this.headerView = headerView
        headerView.setToolbarActionHiddenCallback {
            messagesListView?.getMessageCommandEventListener()?.invoke(MessageCommandEvent.OnCancelMultiselectEvent)
            inputView?.getEventListeners()?.onMultiselectModeListener(false)
        }

        headerView.setSearchBarHiddenCallback {
            inputView?.getEventListeners()?.onSearchModeListener(false)
        }
    }

    fun setInputView(inputView: MessageInputView) {
        this.inputView = inputView
    }

    fun showMessageActions(vararg selectedMessages: SceytMessage): Menu? {
        val messageActionListener = messagesListView?.messageActionsViewClickListeners
                ?: return null
        inputView?.getEventListeners()?.onMultiselectModeListener(true)
        return headerView?.uiElementsListeners?.onShowMessageActionsMenu(*selectedMessages, menuResId = R.menu.sceyt_menu_message_actions) { it, actionFinish ->
            val firstMessage = selectedMessages.getOrNull(0)
            when (it.itemId) {
                R.id.sceyt_edit_message -> firstMessage?.let { message ->
                    actionFinish.invoke()
                    messageActionListener.onEditMessageClick(message)
                }

                R.id.sceyt_forward -> {
                    actionFinish.invoke()
                    messageActionListener.onForwardMessageClick(*selectedMessages)
                }

                R.id.sceyt_reply -> firstMessage?.let { message ->
                    actionFinish.invoke()
                    messageActionListener.onReplyMessageClick(message)
                }

                R.id.sceyt_reply_in_thread -> firstMessage?.let { message ->
                    actionFinish.invoke()
                    messageActionListener.onReplyMessageInThreadClick(message)
                }

                R.id.sceyt_copy_message -> {
                    actionFinish.invoke()
                    messageActionListener.onCopyMessagesClick(*selectedMessages)
                }

                R.id.sceyt_delete_message -> {
                    messageActionListener.onDeleteMessageClick(*selectedMessages,
                        requireForMe = selectedMessages.any { it.incoming }, actionFinish = {
                            actionFinish.invoke()
                        })
                }
            }
        }
    }

    fun hideMessageActions() {
        headerView?.uiElementsListeners?.onHideMessageActionsMenu()
        inputView?.getEventListeners()?.onMultiselectModeListener(false)
    }

    fun cancelMultiSelectMode() {
        headerView?.uiElementsListeners?.onHideMessageActionsMenu()
        inputView?.getEventListeners()?.onMultiselectModeListener(false)
        messagesListView?.cancelMultiSelectMode()
    }

    fun showSearchMessage(event: MessageCommandEvent.SearchMessages){
        headerView?.uiElementsListeners?.showSearchMessagesBar(event)
        inputView?.getEventListeners()?.onSearchModeListener(true)
    }
}