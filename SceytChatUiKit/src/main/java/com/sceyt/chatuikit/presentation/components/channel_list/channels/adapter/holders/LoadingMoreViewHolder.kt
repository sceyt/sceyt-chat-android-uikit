package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

open class LoadingMoreViewHolder<T>(
        binding: SceytItemLoadingMoreBinding,
) : BaseViewHolder<T>(binding.root) {

    init {
        binding.adapterListLoadingProgressBar.setProgressColor(SceytChatUIKit.theme.colors.accentColor)
    }

    override fun bind(item: T) {}
}