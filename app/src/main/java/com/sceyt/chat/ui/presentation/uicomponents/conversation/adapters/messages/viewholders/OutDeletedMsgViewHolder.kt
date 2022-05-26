package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import com.sceyt.chat.ui.databinding.SceytItemOutDeletedMessageBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle

class OutDeletedMsgViewHolder(
        private val binding: SceytItemOutDeletedMessageBinding,
        messageListeners: MessageClickListenersImpl,
) : BaseMsgViewHolder(binding.root, messageListeners) {

    init {
        binding.setMessageItemStyle()
    }

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

    private fun SceytItemOutDeletedMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
        }
    }
}