package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class SearchSectionViewHolder(
    private val style: GlobalSearchStyle,
    private val binding: SceytItemGlobalSearchSectionBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: GlobalSearchListItem.SectionHeader) {
        style.chatsPageStyle.separatorTextStyle.apply(binding.root)
        binding.root.setText(item.titleRes)
    }
}