package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemOutDeletedMessageBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

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
                    val dateText = DateTimeUtil.getDateTimeString(message.createdAt)
                    messageDate.setDateText(dateText, false)
                }
            }
        }
    }

    override val enableReply = false

    override val selectMessageView get() = binding.selectView

    private fun SceytItemOutDeletedMessageBinding.setMessageItemStyle() {
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.outBubbleColor)
        messageBody.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))
    }
}