package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.text.format.DateUtils
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytItemMessageDateSeparatorBinding

class DateSeparatorViewHolder(
        private val binding: SceytItemMessageDateSeparatorBinding
) : BaseMsgViewHolder(binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        if (item is MessageListItem.DateSeparatorItem) {
            val createdAt = item.createdAt
            val dateText = when {
                DateUtils.isToday(createdAt) -> itemView.context.getString(R.string.sceyt_today)
                else -> DateTimeUtil.getDateTimeString(createdAt, "MMMM dd")
            }
            binding.messageDay.text = dateText
        }
    }

    private fun SceytItemMessageDateSeparatorBinding.setMessageItemStyle() {

    }
}