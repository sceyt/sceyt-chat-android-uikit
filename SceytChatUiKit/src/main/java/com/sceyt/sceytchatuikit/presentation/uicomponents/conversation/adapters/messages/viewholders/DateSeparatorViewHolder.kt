package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.text.format.DateUtils
import com.sceyt.sceytchatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.isThisYear
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil

class DateSeparatorViewHolder(
        private val binding: SceytItemMessageDateSeparatorBinding
) : BaseMsgViewHolder(binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.DateSeparatorItem) {
            val createdAt = item.createdAt
            val dateText = when {
                DateUtils.isToday(createdAt) -> MessagesStyle.dateSeparatorDateFormat.today(itemView.context)
                createdAt.isThisYear() -> DateTimeUtil.getDateTimeString(createdAt, MessagesStyle.dateSeparatorDateFormat.thisYear(itemView.context))
                else -> DateTimeUtil.getDateTimeString(createdAt, MessagesStyle.dateSeparatorDateFormat.olderThisYear(itemView.context))
            }
            binding.messageDay.text = dateText
        }
    }

    private fun SceytItemMessageDateSeparatorBinding.setMessageItemStyle() {
        with(root.context) {
            messageDay.background = getCompatDrawable(MessagesStyle.dateSeparatorItemBackground)
            messageDay.setTextColor(getCompatColor(MessagesStyle.dateSeparatorItemTextColor))
        }
    }
}