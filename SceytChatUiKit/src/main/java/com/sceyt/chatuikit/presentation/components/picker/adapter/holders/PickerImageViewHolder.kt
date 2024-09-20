package com.sceyt.chatuikit.presentation.components.picker.adapter.holders

import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemPickerImageBinding
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import com.sceyt.chatuikit.styles.MediaPickerStyle

class PickerImageViewHolder(
        val binding: SceytItemPickerImageBinding,
        style: MediaPickerStyle,
        clickListener: MediaAdapter.MediaClickListener
) : BasePickerViewHolder(binding.root, style, clickListener) {

    private lateinit var item: MediaItem

    init {
        itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun bind(item: MediaItem, diff: GalleryMediaItemDiff) {
        this.item = item

        if (diff.filePathChanged) {
            Glide.with(itemView.context)
                .load(item.media.realPath)
                .override(itemView.width)
                .placeholder(SceytChatUIKit.theme.backgroundColorSecondary)
                .error(R.drawable.sceyt_ic_broken_image)
                .into(binding.ivImage)
        }

        if (diff.checkStateChanged)
            setGalleryItemCheckedState(binding.ivSelect, item.media.selected)
    }
}