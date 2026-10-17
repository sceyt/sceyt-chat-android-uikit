package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import com.sceyt.chatuikit.databinding.SceytItemOutDeletedMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class OutDeletedMessageViewHolder(
        private val binding: SceytItemOutDeletedMessageBinding,
        private val style: MessageItemStyle,
        needMediaDataCallback: ((NeedMediaInfoData) -> Unit)? = null,
) : BaseMessageViewHolder(binding.root, style, needMediaDataCallback = needMediaDataCallback) {

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

     override val incoming: Boolean
        get() = false

    private fun SceytItemOutDeletedMessageBinding.setMessageItemStyle() {
        style.outgoingBubbleBackgroundStyle.apply(layoutDetails)
        style.deletedMessageTextStyle.apply(messageBody)
        style.selectionCheckboxStyle.apply(selectView)
        messageBody.text = style.deletedStateText
    }
}