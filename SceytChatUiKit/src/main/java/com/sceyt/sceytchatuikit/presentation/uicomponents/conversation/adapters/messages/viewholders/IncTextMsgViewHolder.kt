package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.databinding.SceytItemIncTextMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle.INC_DEFAULT_SPACE
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle.INC_EDITED_SPACE

class IncTextMsgViewHolder(
        private val binding: SceytItemIncTextMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListenersImpl?,
        displayedListener: ((SceytMessage) -> Unit)?
) : BaseMsgViewHolder(binding.root, messageListeners, displayedListener) {

    init {
        with(binding) {
            setMessageItemStyle()

            layoutDetails.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
                return@setOnLongClickListener true
            }
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message

                if (diff.edited || diff.bodyChanged) {
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

                if (item.message.canShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
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