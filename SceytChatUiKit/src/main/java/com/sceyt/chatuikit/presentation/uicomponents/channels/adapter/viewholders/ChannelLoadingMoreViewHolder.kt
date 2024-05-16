package com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.viewholders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.getCompatColor

class ChannelLoadingMoreViewHolder(binding: SceytItemLoadingMoreBinding) : BaseChannelViewHolder(binding.root){

    init {
        binding.applyStyle()
    }

    private fun SceytItemLoadingMoreBinding.applyStyle() {
        adapterListLoadingProgressBar.indeterminateDrawable.setTint(context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }
}