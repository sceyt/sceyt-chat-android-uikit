package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import android.view.Menu
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView

internal class MessageActionBridge {
    var messagesListView: MessagesListView? = null
        private set
    var headerView: MessagesListHeaderView? = null
        private set
    var inputView: MessageInputView? = null
        private set

    fun setMessagesListView(messagesListView: MessagesListView) {
        this.messagesListView = messagesListView
    }

    fun setHeaderView(headerView: MessagesListHeaderView) {
        this.headerView = headerView
        headerView.setToolbarActionHiddenCallback {
            messagesListView?.getMessageCommandEventListener()?.invoke(MessageCommandEvent.OnCancelMultiselectEvent)
            inputView?.getEventListeners()?.onMultiselectModeListener(false)
        }

        headerView.setSearchModeChangeListener {
            inputView?.getEventListeners()?.onSearchModeChangeListener(it)
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

                R.id.sceyt_message_info -> firstMessage?.let { message ->
                    actionFinish.invoke()
                    messageActionListener.onMessageInfoClick(message)
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

    fun showSearchMessage(event: MessageCommandEvent.SearchMessages) {
        headerView?.uiElementsListeners?.showSearchMessagesBar(event)
        inputView?.getEventListeners()?.onSearchModeChangeListener(true)
    }
}