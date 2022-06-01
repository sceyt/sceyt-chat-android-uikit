package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.databinding.SceytItemIncTextMessageBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.INC_DEFAULT_SPACE
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.INC_EDITED_SPACE

class IncTextMsgViewHolder(
        private val binding: SceytItemIncTextMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListenersImpl?
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

                    val space = if (message.state == MessageState.Edited) INC_EDITED_SPACE else INC_DEFAULT_SPACE
                    messageBody.text = HtmlCompat.fromHtml("${message.body} $space", HtmlCompat.FROM_HTML_MODE_LEGACY)

                    setMessageDay(message.createdAt, message.showDate, messageDay)
                    setMessageDateText(message.createdAt, messageDate, message.state == MessageState.Edited)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)
                    setReplayCount(tvReplayCount, toReplayLine, item)
                    setOrUpdateReactions(item, rvReactions, viewPool)
                    setReplayedMessageContainer(message, binding.viewReplay)

                    layoutDetails.setOnLongClickListener {
                        messageListeners?.onMessageLongClick(it, item)
                        return@setOnLongClickListener true
                    }

                    if (item.message.canShowAvatarAndName)
                        avatar.setOnClickListener {
                            messageListeners?.onAvatarClick(it, item)
                        }
                }
            }
            MessageListItem.LoadingMoreItem -> return
        }
    }

    private fun SceytItemIncTextMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
        }
    }
}