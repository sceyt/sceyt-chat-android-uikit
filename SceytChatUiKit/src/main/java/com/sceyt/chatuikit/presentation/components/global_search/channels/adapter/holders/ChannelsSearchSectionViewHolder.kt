package com.sceyt.chatuikit.presentation.components.global_search.channels.adapter.holders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.styles.search.ChannelsSearchPageStyle

open class ChannelsSearchSectionViewHolder(
    style: ChannelsSearchPageStyle,
    private val binding: SceytItemGlobalSearchSectionBinding,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        style.separatorTextStyle.apply(binding.root)
    }

    fun bind(item: GlobalSearchListItem.SectionHeader) {
        binding.root.setText(item.titleRes)
    }
}
