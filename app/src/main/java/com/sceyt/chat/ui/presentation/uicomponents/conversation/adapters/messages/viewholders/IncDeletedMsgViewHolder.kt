package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.chat.ui.databinding.SceytUiItemIncDeletedMessageBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class IncDeletedMsgViewHolder(
        private val binding: SceytUiItemIncDeletedMessageBinding,
        messageListeners: MessageClickListenersImpl
) : BaseMsgViewHolder(binding.root, messageListeners) {

    override fun bindViews(item: MessageListItem) {
        when (item) {
            is MessageListItem.MessageItem -> {
                with(binding) {
                    val message = item.message
                    this.message = message

                    setMessageDay(message.createdAt, message.showDate, messageDay)
                    setMessageDateText(message.createdAt, messageDate, false)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)
                }
            }
            MessageListItem.LoadingMoreItem -> return
        }
    }
}