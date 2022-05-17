package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.fromHtml
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytUiItemOutDeletedMessageBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.OUT_DEFAULT_SPACE
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.OUT_EDITED_SPACE

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

                    val space = if (message.state == MessageState.Edited) OUT_EDITED_SPACE else OUT_DEFAULT_SPACE
                    messageBody.text = fromHtml("${itemView.context.getString(R.string.message_was_deleted)} $space",
                        HtmlCompat.FROM_HTML_MODE_LEGACY)

                    setMessageDay(message.createdAt, message.showDate, binding.messageDay)
                    setMessageDateText(message.createdAt, messageDate, message.state == MessageState.Edited)
                }
            }
            MessageListItem.LoadingMoreItem -> return
        }
    }
}