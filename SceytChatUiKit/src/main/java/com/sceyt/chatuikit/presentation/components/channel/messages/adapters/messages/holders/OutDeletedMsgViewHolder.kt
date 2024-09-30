package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import com.sceyt.chatuikit.databinding.SceytItemOutDeletedMessageBinding
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class OutDeletedMsgViewHolder(
        private val binding: SceytItemOutDeletedMessageBinding,
        private val style: MessageItemStyle
) : BaseMsgViewHolder(binding.root, style) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message

                if (diff.statusChanged || diff.edited) {
                    setMessageStatusAndDateText(message, messageDate)
                }
            }
        }
    }

    override val enableReply = false

    override val selectMessageView get() = binding.selectView

    override val incoming: Boolean = false

    private fun SceytItemOutDeletedMessageBinding.setMessageItemStyle() {
        layoutDetails.setBackgroundTint(style.outgoingBubbleColor)
        style.deletedMessageTextStyle.apply(messageBody)
        messageBody.text = style.deletedStateText
    }
}