package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.view.View
import com.sceyt.chatuikit.databinding.SceytItemSystemMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class SystemMessageViewHolder(
    val binding: SceytItemSystemMessageBinding,
    style: MessageItemStyle,
    displayedListener: ((MessageListItem) -> Unit)?,
) : BaseMessageViewHolder(binding.root, style, displayedListener = displayedListener) {

    private val systemMessageStyle = style.systemMessageItemStyle

    init {
        binding.applyStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message
                title.text = systemMessageStyle.textFormatter.format(context, message)
            }
        }
    }

    override val enableReply = false
    override val incoming: Boolean
        get() = false

    override val selectMessageView: View? = null

    private fun SceytItemSystemMessageBinding.applyStyle() {
        with(systemMessageStyle) {
            textStyle.apply(title)
            backgroundStyle.apply(title)
        }
    }
}