package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.chat.ui.databinding.SceytUiItemOutDeletedMessageBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class OutDeletedMsgViewHolder(
        private val binding: SceytUiItemOutDeletedMessageBinding,
        messageListeners: MessageClickListenersImpl,
) : BaseMsgViewHolder(binding.root, messageListeners) {

    override fun bindViews(item: MessageListItem) {
        when (item) {
            is MessageListItem.MessageItem -> {
                with(binding) {
                    val message = item.message
                    this.message = message

                    setMessageDay(message.createdAt, message.showDate, binding.messageDay)
                    setMessageDateText(message.createdAt, messageDate, false)
                }
            }
            MessageListItem.LoadingMoreItem -> return
        }
    }
}