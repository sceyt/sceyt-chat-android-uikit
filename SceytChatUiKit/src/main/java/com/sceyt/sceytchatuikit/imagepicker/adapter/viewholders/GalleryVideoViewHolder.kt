package com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.ItemGalleryVideoBinding
import com.sceyt.sceytchatuikit.imagepicker.adapter.GalleryMediaAdapter
import com.sceyt.sceytchatuikit.imagepicker.adapter.MediaItem

class GalleryVideoViewHolder(val binding: ItemGalleryVideoBinding,
                             clickListener: GalleryMediaAdapter.MediaClickListener) : BaseGalleryViewHolder(binding.root, clickListener) {

    override fun bind(item: MediaItem) {
        val data = item.media
        binding.data = data

        binding.tvDuration.isVisible = data.isWrongImage.not()

        if (data.isWrongImage) {
            binding.ivImage.setImageResource(R.drawable.ic_broken_image)
        } else {
            Glide.with(context)
                .load(item.media.realPath)
                .error(R.drawable.ic_broken_image)
                .into(binding.ivImage)

            binding.tvDuration.text = millisecondsToTime((item as MediaItem.Video).duration.toLong())
        }

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
}