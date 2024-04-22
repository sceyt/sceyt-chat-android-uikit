package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemOutTextMessageBinding
import com.sceyt.chatuikit.extensions.setTextAndDrawableByColorId
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle

class OutTextMsgViewHolder(
        private val binding: SceytItemOutTextMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        private val style: MessagesListViewStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        userNameBuilder: ((User) -> String)?
) : BaseMsgViewHolder(binding.root, style, messageListeners, userNameBuilder = userNameBuilder) {

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                messageListeners?.onMessageClick(it, messageListItem as MessageListItem.MessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
                return@setOnLongClickListener true
            }

            messageBody.doOnLongClick {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
            }

            messageBody.doOnClickWhenNoLink {
                messageListeners?.onMessageClick(it, messageListItem as MessageListItem.MessageItem)
            }
        }
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)

        if (!diff.hasDifference()) return

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message
                tvForwarded.isVisible = message.isForwarded

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.edited || diff.bodyChanged) {
                    setMessageBody(messageBody, message, false)
                    setBodyTextPosition(messageBody, messageDate, layoutDetails)
                }

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)
            }
        }
    }

    override val selectMessageView get() = binding.selectView

    private fun SceytItemOutTextMessageBinding.setMessageItemStyle() {
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.outBubbleColor)
        tvForwarded.setTextAndDrawableByColorId(SceytChatUIKit.theme.accentColor)
        messageBody.setLinkTextColor(style.autoLinkTextColor)
    }

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, true)
}