package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle

class LoadingMoreMessagesViewHolder(binding: SceytItemLoadingMoreBinding,
                                    style: MessageItemStyle) : BaseMsgViewHolder(binding.root, style) {

    override val enableReply = false
}