package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.text.format.DateUtils
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.utils.DateTimeUtil

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
                DateUtils.isToday(createdAt) -> itemView.context.getString(R.string.today)
                else -> DateTimeUtil.getDateTimeString(createdAt, "MMMM dd")
            }
            binding.messageDay.text = dateText
        }
    }

    private fun SceytItemMessageDateSeparatorBinding.setMessageItemStyle() {

    }
}