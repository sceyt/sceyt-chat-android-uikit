package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColorRes
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

open class LoadingMoreViewHolder<T>(
        binding: SceytItemLoadingMoreBinding,
) : BaseViewHolder<T>(binding.root) {

    init {
        binding.adapterListLoadingProgressBar.setProgressColorRes(SceytChatUIKit.theme.colors.accentColor)
    }

    override fun bind(item: T) {}
}