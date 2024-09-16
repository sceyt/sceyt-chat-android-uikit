package com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemPickerImageBinding
import com.sceyt.chatuikit.databinding.SceytItemPickerVideoBinding
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.MediaItem
import com.sceyt.chatuikit.sceytstyles.MediaPickerStyle

open class MediaPickerItemViewHolderFactory(
        private val style: MediaPickerStyle,
        private val clickListener: MediaAdapter.MediaClickListener
) {

    open fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasePickerViewHolder {
        return when (viewType) {
            MediaAdapter.ViewType.Image.ordinal -> createImageViewHolder(parent)
            MediaAdapter.ViewType.Video.ordinal -> createVideoViewHolder(parent)
            else -> throw Exception("Unsupported view type")
        }
    }

    private fun createImageViewHolder(parent: ViewGroup): PickerImageViewHolder {
        val binding = SceytItemPickerImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PickerImageViewHolder(binding, style, clickListener)
    }

    private fun createVideoViewHolder(parent: ViewGroup): PickerVideoViewHolder {
        val binding = SceytItemPickerVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PickerVideoViewHolder(binding, style, clickListener)
    }

    open fun getItemViewType(item: MediaItem): Int {
        return when (item) {
            is MediaItem.Image -> MediaAdapter.ViewType.Image.ordinal
            is MediaItem.Video -> MediaAdapter.ViewType.Video.ordinal
        }
    }
}