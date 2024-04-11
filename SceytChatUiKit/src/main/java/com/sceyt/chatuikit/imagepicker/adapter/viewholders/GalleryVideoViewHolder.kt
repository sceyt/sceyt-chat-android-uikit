package com.sceyt.chatuikit.imagepicker.adapter.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytItemGalleryVideoBinding
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.imagepicker.adapter.GalleryMediaAdapter
import com.sceyt.chatuikit.imagepicker.adapter.MediaItem
import com.sceyt.chatuikit.sceytstyles.GalleryPickerStyle

class GalleryVideoViewHolder(val binding: SceytItemGalleryVideoBinding,
                             clickListener: GalleryMediaAdapter.MediaClickListener) : BaseGalleryViewHolder(binding.root, clickListener) {

    init {
        binding.setupStyle()
    }

    override fun bind(item: MediaItem) {
        val data = item.media
        binding.data = data

        binding.tvDuration.isVisible = data.isWrong.not()

        Glide.with(itemView.context)
            .load(item.media.realPath)
            .override(itemView.width)
            .placeholder(R.color.sceyt_gallery_item_default_color)
            .error(R.drawable.sceyt_ic_broken_image)
            .into(binding.ivImage)

        binding.tvDuration.text = millisecondsToTime((item as MediaItem.Video).duration.toLong())

        itemView.setOnClickListener {
            onItemClick(item)
        }

        binding.ivSelect.setOnClickListener {
            onItemClick(item)
        }
    }

    private fun millisecondsToTime(milliseconds: Long): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = milliseconds / 1000 % 60
        val secondsStr = seconds.toString()

        val secs: String = if (secondsStr.length >= 2) {
            secondsStr.substring(0, 2)
        } else {
            "0$secondsStr"
        }
        return "$minutes:$secs"
    }

    private fun SceytItemGalleryVideoBinding.setupStyle() {
        tvDuration.setDrawableStart(GalleryPickerStyle.videoDurationIcon)
    }
}