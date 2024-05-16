package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle

class LoadingMoreMessagesViewHolder(binding: SceytItemLoadingMoreBinding,
                                    style: MessageItemStyle) : BaseMsgViewHolder(binding.root, style) {

    init {
        binding.applyStyle()
    }

    private fun SceytItemLoadingMoreBinding.applyStyle() {
        adapterListLoadingProgressBar.setProgressColor(SceytChatUIKit.theme.accentColor)
    }

    override val enableReply = false
}