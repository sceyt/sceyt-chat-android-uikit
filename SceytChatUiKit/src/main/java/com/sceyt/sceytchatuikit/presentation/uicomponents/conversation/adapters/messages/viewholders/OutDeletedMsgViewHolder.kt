package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import com.sceyt.sceytchatuikit.databinding.SceytItemOutDeletedMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil

class OutDeletedMsgViewHolder(
        private val binding: SceytItemOutDeletedMessageBinding
) : BaseMsgViewHolder(binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
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
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
        }
    }
}