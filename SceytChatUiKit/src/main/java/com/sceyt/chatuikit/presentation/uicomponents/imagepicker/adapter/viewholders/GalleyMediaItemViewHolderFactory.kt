package com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemGalleryImageBinding
import com.sceyt.chatuikit.databinding.SceytItemGalleryVideoBinding
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.GalleryMediaAdapter
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.MediaItem
import com.sceyt.chatuikit.sceytstyles.GalleryPickerStyle

open class GalleyMediaItemViewHolderFactory(
        private val style: GalleryPickerStyle,
        private val clickListener: GalleryMediaAdapter.MediaClickListener
) {

    open fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseGalleryViewHolder {
        return when (viewType) {
            GalleryMediaAdapter.ViewType.Image.ordinal -> createImageViewHolder(parent)
            GalleryMediaAdapter.ViewType.Video.ordinal -> createVideoViewHolder(parent)
            else -> throw Exception("Unsupported view type")
        }
    }

    private fun createImageViewHolder(parent: ViewGroup): GalleryImageViewHolder {
        val binding = SceytItemGalleryImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GalleryImageViewHolder(binding, style, clickListener)
    }

    private fun createVideoViewHolder(parent: ViewGroup): GalleryVideoViewHolder {
        val binding = SceytItemGalleryVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GalleryVideoViewHolder(binding, style, clickListener)
    }

    open fun getItemViewType(item: MediaItem): Int {
        return when (item) {
            is MediaItem.Image -> GalleryMediaAdapter.ViewType.Image.ordinal
            is MediaItem.Video -> GalleryMediaAdapter.ViewType.Video.ordinal
        }
    }
}