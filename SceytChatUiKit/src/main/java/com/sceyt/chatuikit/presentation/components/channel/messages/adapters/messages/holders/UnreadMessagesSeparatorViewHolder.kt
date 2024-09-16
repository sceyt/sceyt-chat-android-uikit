package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import com.sceyt.chatuikit.databinding.SceytItemUnreadMessagesSeparatorBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.styles.MessagesListViewStyle

class UnreadMessagesSeparatorViewHolder(
        binding: SceytItemUnreadMessagesSeparatorBinding,
        private val style: MessagesListViewStyle
) : BaseMsgViewHolder(view = binding.root, style.messageItemStyle) {

    init {
        binding.setMessageItemStyle()
    }

    override val enableReply = false

    private fun SceytItemUnreadMessagesSeparatorBinding.setMessageItemStyle() {
        with(tvUnreadMessagesSeparator) {
            setTextColor(style.unreadMessagesTextColor)
            setBackgroundColor(style.unreadMessagesBackendColor)
            setTypeface(typeface, style.unreadMessagesSeparatorTextStyle)
        }
    }
}