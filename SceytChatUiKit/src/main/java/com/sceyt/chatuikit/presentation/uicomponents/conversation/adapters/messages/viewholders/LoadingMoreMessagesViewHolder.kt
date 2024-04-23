package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle

class LoadingMoreMessagesViewHolder(private val binding: SceytItemLoadingMoreBinding,
                                    style: MessageItemStyle) : BaseMsgViewHolder(binding.root, style) {

    init {
        binding.setupStyle()
    }

    private fun SceytItemLoadingMoreBinding.setupStyle() {
        adapterListLoadingProgressBar.indeterminateDrawable.setTint(context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }

    override val enableReply = false
}