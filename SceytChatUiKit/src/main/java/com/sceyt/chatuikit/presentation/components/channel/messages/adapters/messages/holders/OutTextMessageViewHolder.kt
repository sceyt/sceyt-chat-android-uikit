package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemOutTextMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class OutTextMessageViewHolder(
        private val binding: SceytItemOutTextMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
) : BaseMessageViewHolder(binding.root, style, messageListeners) {

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

                if (diff.reactionsChanged || diff.edited)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)
            }
        }
    }

    override val selectMessageView get() = binding.selectView

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, true)

    override val incoming: Boolean
        get() = false

    private fun SceytItemOutTextMessageBinding.setMessageItemStyle() {
        applyCommonStyle(
            layoutDetails = layoutDetails,
            tvForwarded = tvForwarded,
            messageBody = messageBody,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine
        )
    }
}