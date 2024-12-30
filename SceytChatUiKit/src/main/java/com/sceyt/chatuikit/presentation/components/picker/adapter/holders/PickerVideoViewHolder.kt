package com.sceyt.chatuikit.presentation.components.picker.adapter.holders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.databinding.SceytItemPickerVideoBinding
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import com.sceyt.chatuikit.styles.media_picker.MediaPickerItemStyle
import kotlin.time.Duration.Companion.milliseconds

class PickerVideoViewHolder(
        private val binding: SceytItemPickerVideoBinding,
        private val style: MediaPickerItemStyle,
        clickListener: MediaAdapter.MediaClickListener,
) : BasePickerViewHolder(binding.root, clickListener) {

    private lateinit var mediaItem: MediaItem.Video

    init {
        binding.applyStyle()

        itemView.setOnClickListener {
            onItemClick(mediaItem)
        }
    }

    override fun bind(item: MediaItem, diff: GalleryMediaItemDiff) {
        mediaItem = item as MediaItem.Video

        if (diff.filePathChanged) {
            Glide.with(itemView.context)
                .load(item.media.realPath)
                .override(itemView.width)
                .error(style.brokenMediaPlaceHolder)
                .into(binding.ivImage)
        }

        if (diff.checkStateChanged)
            binding.checkbox.isChecked = item.media.selected

        binding.tvDuration.isVisible = item.media.isWrong.not()
        binding.tvDuration.text = style.mediaDurationFormatter.format(context, mediaItem.duration.milliseconds.inWholeSeconds)
    }

    private fun SceytItemPickerVideoBinding.applyStyle() {
        ivImage.setBackgroundColor(style.backgroundColor)
        tvDuration.setDrawableStart(style.videoDurationIcon)
        style.videoDurationTextStyle.apply(tvDuration)
        style.checkboxStyle.apply(checkbox)
    }
}