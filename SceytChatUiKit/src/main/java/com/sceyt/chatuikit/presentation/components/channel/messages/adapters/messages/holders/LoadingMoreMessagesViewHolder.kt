package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class LoadingMoreMessagesViewHolder(binding: SceytItemLoadingMoreBinding,
                                    style: MessageItemStyle) : BaseMsgViewHolder(binding.root, style) {

    init {
        binding.applyStyle()
    }

    private fun SceytItemLoadingMoreBinding.applyStyle() {
        adapterListLoadingProgressBar.setProgressColor(SceytChatUIKit.theme.colors.accentColor)
    }

    override val enableReply = false

    override val incoming = false
}