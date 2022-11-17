package com.sceyt.sceytchatuikit.imagepicker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.sceytchatuikit.databinding.ItemGalleryImageBinding
import com.sceyt.sceytchatuikit.databinding.ItemGalleryVideoBinding
import com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders.BaseGalleryViewHolder
import com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders.GalleryImageViewHolder
import com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders.GalleryVideoViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.GalleryPickerStyle

class GalleryMediaAdapter(private var clickListener: MediaClickListener) : ListAdapter<MediaItem, BaseGalleryViewHolder>(DIFF_UTIL) {

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem == newItem
            }
        }

        @BindingAdapter("setMediaCheckedState")
        @JvmStatic
        fun setMediaCheckedState(image: ImageView, isChecked: Boolean) {
            if (isChecked) {
                image.setImageResource(GalleryPickerStyle.checkedStateIcon)
            } else {
                image.setImageResource(GalleryPickerStyle.unCheckedStateIcon)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseGalleryViewHolder {
        return when (viewType) {
            ViewType.Image.ordinal -> GalleryImageViewHolder(ItemGalleryImageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false), clickListener)

            ViewType.Video.ordinal -> GalleryVideoViewHolder(ItemGalleryVideoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false), clickListener)

            else -> throw Exception("Unsupported view type")
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is MediaItem.Image -> ViewType.Image.ordinal
            is MediaItem.Video -> ViewType.Video.ordinal
        }
    }

    override fun onBindViewHolder(holder: BaseGalleryViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    fun interface MediaClickListener {
        fun onClick(item: MediaItem)
    }

    enum class ViewType {
        Image, Video
    }
}