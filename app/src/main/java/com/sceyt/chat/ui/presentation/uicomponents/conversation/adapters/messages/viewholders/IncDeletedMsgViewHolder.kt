package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import androidx.core.text.HtmlCompat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytUiItemIncDeletedMessageBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.INC_DEFAULT_SPACE
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.INC_EDITED_SPACE

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

                    val space = if (message.state == MessageState.Edited) INC_EDITED_SPACE else INC_DEFAULT_SPACE
                    messageBody.text = HtmlCompat.fromHtml("${itemView.context.getString(R.string.message_was_deleted)} $space",
                        HtmlCompat.FROM_HTML_MODE_LEGACY)

                    setMessageDay(message.createdAt, message.showDate, messageDay)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)
                }
            }
            MessageListItem.LoadingMoreItem -> return
        }
    }
}