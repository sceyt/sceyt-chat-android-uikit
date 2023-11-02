package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.text.format.DateUtils
import androidx.core.content.res.ResourcesCompat
import com.sceyt.sceytchatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.isThisYear
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.dateformaters.DateFormatData
import java.text.SimpleDateFormat
import java.util.*

class DateSeparatorViewHolder(
        private val binding: SceytItemMessageDateSeparatorBinding
) : BaseMsgViewHolder(view = binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.DateSeparatorItem) {
            val createdAt = item.createdAt
            val dateText = when {
                DateUtils.isToday(createdAt) -> getDateText(createdAt, MessagesStyle.dateSeparatorDateFormat.today(context))
                createdAt.isThisYear() -> getDateText(createdAt, MessagesStyle.dateSeparatorDateFormat.thisYear(context))
                else -> getDateText(createdAt, MessagesStyle.dateSeparatorDateFormat.olderThisYear(context))
            }
            binding.messageDay.text = dateText
        }
    }

    override val enableReply = false

    private fun getDateText(createdAt: Long, data: DateFormatData): String {
        if (data.format == null)
            return "${data.beginTittle}${data.endTitle}"

        return try {
            val simpleDateFormat = SimpleDateFormat(data.format, Locale.getDefault())
            "${data.beginTittle}${simpleDateFormat.format(Date(createdAt))}${data.endTitle}"
        } catch (ex: Exception) {
            "${data.beginTittle}${data.format}${data.endTitle}"
        }
    }

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