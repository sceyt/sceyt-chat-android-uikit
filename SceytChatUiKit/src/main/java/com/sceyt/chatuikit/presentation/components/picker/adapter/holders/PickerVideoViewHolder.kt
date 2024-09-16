package com.sceyt.chatuikit.presentation.components.picker.adapter.holders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytItemPickerVideoBinding
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import com.sceyt.chatuikit.styles.MediaPickerStyle

class PickerVideoViewHolder(
        private val binding: SceytItemPickerVideoBinding,
        private val style: MediaPickerStyle,
        clickListener: MediaAdapter.MediaClickListener
) : BasePickerViewHolder(binding.root, style, clickListener) {

    private lateinit var item: MediaItem

    init {
        binding.applyStyle()

        itemView.setOnClickListener {
            onItemClick(item)
        }

        binding.ivSelect.setOnClickListener {
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

        binding.tvDuration.isVisible = item.media.isWrong.not()
        binding.tvDuration.text = millisecondsToTime((item as MediaItem.Video).duration.toLong())
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

    private fun SceytItemPickerVideoBinding.applyStyle() {
        tvDuration.setDrawableStart(style.videoDurationIcon)
    }
}