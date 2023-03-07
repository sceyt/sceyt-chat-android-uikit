package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytItemUnreadMessagesSeparatorBinding

class UnreadMessagesSeparatorViewHolder(
        binding: SceytItemUnreadMessagesSeparatorBinding
) : BaseMsgViewHolder(view = binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    private fun SceytItemUnreadMessagesSeparatorBinding.setMessageItemStyle() {
        with(context) {
            /* messageDay.apply {
                 background = getCompatDrawable(MessagesStyle.dateSeparatorItemBackground)
                 setTextColor(getCompatColor(MessagesStyle.dateSeparatorItemTextColor))
                 val dateTypeface = if (MessagesStyle.dateSeparatorTextFont != -1)
                     ResourcesCompat.getFont(this@with, MessagesStyle.dateSeparatorTextFont) else typeface
                 setTypeface(dateTypeface, MessagesStyle.dateSeparatorTextStyle)
             }*/
        }
    }
}