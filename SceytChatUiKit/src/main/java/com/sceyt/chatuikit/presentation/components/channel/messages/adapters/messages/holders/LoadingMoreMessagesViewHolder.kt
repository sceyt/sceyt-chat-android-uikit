package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.view.View
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColorRes
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class LoadingMoreMessagesViewHolder(
        binding: SceytItemLoadingMoreBinding,
        style: MessageItemStyle,
) : BaseMessageViewHolder(binding.root, style) {

    init {
        binding.applyStyle()
    }

    private fun SceytItemLoadingMoreBinding.applyStyle() {
        adapterListLoadingProgressBar.setProgressColorRes(SceytChatUIKit.theme.colors.accentColor)
    }

    override val enableReply = false

    override val incoming: Boolean
        get() = false

    override val selectMessageView: View?
        get() = null
}