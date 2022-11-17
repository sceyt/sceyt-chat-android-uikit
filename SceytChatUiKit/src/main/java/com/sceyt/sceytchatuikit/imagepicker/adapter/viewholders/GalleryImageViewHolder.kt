package com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders

import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.ItemGalleryImageBinding
import com.sceyt.sceytchatuikit.imagepicker.adapter.GalleryMediaAdapter
import com.sceyt.sceytchatuikit.imagepicker.adapter.MediaItem

class GalleryImageViewHolder(val binding: ItemGalleryImageBinding,
                             clickListener: GalleryMediaAdapter.MediaClickListener) : BaseGalleryViewHolder(binding.root, clickListener) {

    override fun bind(item: MediaItem) {
        val data = item.media
        binding.data = data

        if (data.isWrongImage) {
            binding.ivImage.setImageResource(R.drawable.ic_broken_image)
        } else {
            Glide.with(context)
                .load(item.media.contentUri)
                .error(R.drawable.ic_broken_image)
                .into(binding.ivImage)
        }

        itemView.setOnClickListener {
            onItemClick(item)
        }
    }
}