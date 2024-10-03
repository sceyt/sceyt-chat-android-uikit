package com.sceyt.chatuikit.presentation.components.picker.adapter.holders

import com.bumptech.glide.Glide
import com.sceyt.chatuikit.databinding.SceytItemPickerImageBinding
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import com.sceyt.chatuikit.styles.media_picker.MediaPickerItemStyle

class PickerImageViewHolder(
        private val binding: SceytItemPickerImageBinding,
        private val style: MediaPickerItemStyle,
        clickListener: MediaAdapter.MediaClickListener
) : BasePickerViewHolder(binding.root, clickListener) {

    private lateinit var item: MediaItem

    init {
        binding.applyStyle()

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
                .error(style.brokenMediaPlaceHolder)
                .into(binding.ivImage)
        }

        if (diff.checkStateChanged)
            binding.checkbox.isChecked = item.media.selected
    }

    private fun SceytItemPickerImageBinding.applyStyle() {
        ivImage.setBackgroundColor(style.backgroundColor)
        style.checkboxStyle.apply(checkbox)
    }
}