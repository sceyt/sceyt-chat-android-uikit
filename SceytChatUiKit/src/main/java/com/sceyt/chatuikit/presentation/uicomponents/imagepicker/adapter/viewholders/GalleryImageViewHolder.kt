package com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.viewholders

import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytItemGalleryImageBinding
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.GalleryMediaAdapter
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.MediaItem
import com.sceyt.chatuikit.sceytstyles.GalleryPickerStyle

class GalleryImageViewHolder(val binding: SceytItemGalleryImageBinding,
                             style: GalleryPickerStyle,
                             clickListener: GalleryMediaAdapter.MediaClickListener
) : BaseGalleryViewHolder(binding.root, style, clickListener) {

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
                .placeholder(R.color.sceyt_gallery_item_default_color)
                .error(R.drawable.sceyt_ic_broken_image)
                .into(binding.ivImage)
        }

        if (diff.checkStateChanged)
            setGalleryItemCheckedState(binding.ivSelect, item.media.selected)
    }
}