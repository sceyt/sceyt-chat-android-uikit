package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.styles.search.ChatsSearchPageStyle

open class SearchSectionViewHolder(
    private val style: ChatsSearchPageStyle,
    private val binding: SceytItemGlobalSearchSectionBinding,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.applyStyle()
    }

    fun bind(item: GlobalSearchListItem.SectionHeader) {
        binding.root.setText(item.titleRes)
    }

    private fun SceytItemGlobalSearchSectionBinding.applyStyle() {
        style.separatorTextStyle.apply(root)
    }
}
