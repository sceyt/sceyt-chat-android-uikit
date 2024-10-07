package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.view.View
import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import java.util.Date

class DateSeparatorViewHolder(
        private val binding: SceytItemMessageDateSeparatorBinding,
        listStyle: MessagesListViewStyle,
) : BaseMessageViewHolder(view = binding.root, itemStyle = listStyle.messageItemStyle) {
    private val style = listStyle.dateSeparatorStyle

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        itemView.isVisible = true
        if (item is MessageListItem.DateSeparatorItem) {
            val createdAt = item.createdAt
            binding.messageDay.text = style.dateFormatter.format(context, Date(createdAt))
        }
    }

    fun showHide(show: Boolean) {
        binding.messageDay.alpha = if (show) 1f else 0f
    }

    override val enableReply: Boolean
        get() = false

    override val incoming: Boolean
        get() = false

    override val selectMessageView: View?
        get() = null

    private fun SceytItemMessageDateSeparatorBinding.setMessageItemStyle() {
        style.textStyle.apply(messageDay)
        style.backgroundStyle.apply(messageDay)
    }
}