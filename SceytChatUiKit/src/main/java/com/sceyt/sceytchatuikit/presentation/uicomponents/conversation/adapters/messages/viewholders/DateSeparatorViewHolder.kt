package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isInvisible
import com.sceyt.sceytchatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.persistence.differs.MessageDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil

class DateSeparatorViewHolder(
        private val binding: SceytItemMessageDateSeparatorBinding
) : BaseMsgViewHolder(view = binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        itemView.isInvisible = false
        if (item is MessageListItem.DateSeparatorItem) {
            val createdAt = item.createdAt
            val dateText = DateTimeUtil.getDateTimeStringWithDateFormatter(
                context = context,
                time = createdAt,
                dateFormatter = MessagesStyle.dateSeparatorDateFormat
            )
            binding.messageDay.text = dateText
        }
    }

    fun showHide(show: Boolean) {
        binding.messageDay.alpha = if (show) 1f else 0f
    }

    override val enableReply = false

    private fun SceytItemMessageDateSeparatorBinding.setMessageItemStyle() {
        with(context) {
            messageDay.apply {
                background = getCompatDrawable(MessagesStyle.dateSeparatorItemBackground)
                setTextColor(getCompatColor(MessagesStyle.dateSeparatorItemTextColor))
                val dateTypeface = if (MessagesStyle.dateSeparatorTextFont != -1)
                    ResourcesCompat.getFont(this@with, MessagesStyle.dateSeparatorTextFont) else typeface
                setTypeface(dateTypeface, MessagesStyle.dateSeparatorTextStyle)
            }
        }
    }
}