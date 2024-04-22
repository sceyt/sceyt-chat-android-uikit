package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

class DateSeparatorViewHolder(
        private val binding: SceytItemMessageDateSeparatorBinding,
        private val style: MessagesListViewStyle,
) : BaseMsgViewHolder(view = binding.root, style = style) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        itemView.isVisible = true
        if (item is MessageListItem.DateSeparatorItem) {
            val createdAt = item.createdAt
            val dateText = DateTimeUtil.getDateTimeStringWithDateFormatter(
                context = context,
                time = createdAt,
                dateFormatter = style.dateSeparatorDateFormat
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
                backgroundTintList = ColorStateList.valueOf(style.dateSeparatorItemBackgroundColor)
                setTextColor(style.dateSeparatorItemTextColor)
                val dateTypeface = if (style.dateSeparatorTextFont != -1)
                    ResourcesCompat.getFont(this@with, style.dateSeparatorTextFont) else typeface
                setTypeface(dateTypeface, style.dateSeparatorTextStyle)
            }
        }
    }
}