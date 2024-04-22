package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle

class LoadingMoreMessagesViewHolder(binding: SceytItemLoadingMoreBinding,
                                    style: MessagesListViewStyle) : BaseMsgViewHolder(binding.root, style) {

    override val enableReply = false
}