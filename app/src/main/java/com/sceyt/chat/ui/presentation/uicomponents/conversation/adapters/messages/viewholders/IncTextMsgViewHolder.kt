package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.databinding.SceytItemIncTextMessageBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
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

    private lateinit var messageItem: MessageListItem.MessageItem

    init {
        with(binding) {
            setMessageItemStyle()

            layoutDetails.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageItem)
                return@setOnLongClickListener true
            }
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        if (item is MessageListItem.MessageItem) {
            with(binding) {
                messageItem = item
                val message = item.message

                if (diff.edited) {
                    val space = if (message.state == MessageState.Edited) INC_EDITED_SPACE else INC_DEFAULT_SPACE
                    messageBody.text = HtmlCompat.fromHtml("${message.body} $space", HtmlCompat.FROM_HTML_MODE_LEGACY)
                }

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replayCountChanged)
                    setReplayCount(tvReplayCount, toReplayLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (diff.replayContainerChanged)
                    setReplayedMessageContainer(message, viewReplay)

                if (messageItem.message.canShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, messageItem)
                    }
            }
        }
    }

    private fun SceytItemIncTextMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
        }
    }
}