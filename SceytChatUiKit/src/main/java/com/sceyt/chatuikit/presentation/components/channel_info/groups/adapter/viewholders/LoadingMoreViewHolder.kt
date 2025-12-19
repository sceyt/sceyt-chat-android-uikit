package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.viewholders

import androidx.annotation.ColorInt
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.CommonGroupListItem
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class LoadingMoreViewHolder(
    binding: SceytItemLoadingMoreBinding,
    @ColorInt loadMoreProgressColor: Int
) : BaseViewHolder<CommonGroupListItem>(binding.root) {

    init {
        binding.adapterListLoadingProgressBar.setProgressColor(loadMoreProgressColor)
    }

    override fun bind(item: CommonGroupListItem) = Unit
}