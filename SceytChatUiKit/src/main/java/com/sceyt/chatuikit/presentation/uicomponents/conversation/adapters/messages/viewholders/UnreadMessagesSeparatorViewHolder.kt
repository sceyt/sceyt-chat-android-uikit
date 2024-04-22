package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.chatuikit.databinding.SceytItemUnreadMessagesSeparatorBinding
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle

class UnreadMessagesSeparatorViewHolder(
        binding: SceytItemUnreadMessagesSeparatorBinding,
        private val style: MessagesListViewStyle
) : BaseMsgViewHolder(view = binding.root, style) {

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