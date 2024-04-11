package com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.viewholders

import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytItemGalleryImageBinding
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.GalleryMediaAdapter
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.MediaItem

class GalleryImageViewHolder(val binding: SceytItemGalleryImageBinding,
                             clickListener: GalleryMediaAdapter.MediaClickListener) : BaseGalleryViewHolder(binding.root, clickListener) {

    override fun bind(item: MediaItem) {
        val data = item.media
        binding.data = data

        Glide.with(itemView.context)
            .load(item.media.realPath)
            .override(itemView.width)
            .placeholder(R.color.sceyt_gallery_item_default_color)
            .error(R.drawable.sceyt_ic_broken_image)
            .into(binding.ivImage)

        itemView.setOnClickListener {
            onItemClick(item)
        }
    }
}