package com.sceyt.chatuikit.presentation.components.global_search.media.adapter.holders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.styles.common.DateSeparatorStyle
import java.util.Date

open class MediaSearchSectionViewHolder(
    private val style: DateSeparatorStyle,
    private val binding: SceytItemGlobalSearchSectionBinding,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.applyStyle()
    }

    fun bind(item: GlobalSearchListItem.DateSeparator) {
        binding.root.text = style.dateFormatter.format(binding.root.context, Date(item.timestamp))
    }

    private fun SceytItemGlobalSearchSectionBinding.applyStyle() {
        style.backgroundStyle.apply(root)
        style.textStyle.apply(root)
    }
}
