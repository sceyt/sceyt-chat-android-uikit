package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import com.sceyt.chatuikit.databinding.SceytItemUnreadMessagesSeparatorBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle

class UnreadMessagesSeparatorViewHolder(
        binding: SceytItemUnreadMessagesSeparatorBinding,
        listViewStyle: MessagesListViewStyle
) : BaseMsgViewHolder(view = binding.root, listViewStyle.messageItemStyle) {
    private val style = listViewStyle.unreadMessagesSeparatorStyle

    init {
        binding.setMessageItemStyle()
    }

    override val enableReply = false

    private fun SceytItemUnreadMessagesSeparatorBinding.setMessageItemStyle() {
        with(tvUnreadMessagesSeparator) {
            style.textStyle.apply(this)

            if (style.backgroundColor != UNSET_COLOR)
                setBackgroundColor(style.backgroundColor)
        }
    }
}