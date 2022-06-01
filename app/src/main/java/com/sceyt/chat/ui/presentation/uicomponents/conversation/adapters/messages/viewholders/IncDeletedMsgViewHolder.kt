package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import com.sceyt.chat.ClientWrapper.messageListeners
import com.sceyt.chat.ui.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle

class IncDeletedMsgViewHolder(
        private val binding: SceytItemIncDeletedMessageBinding
) : BaseMsgViewHolder(binding.root) {

    init {
        binding.setMessageItemStyle()
    }

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

    private fun SceytItemIncDeletedMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
        }
    }
}