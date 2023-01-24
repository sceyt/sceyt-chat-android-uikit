package com.sceyt.sceytchatuikit.imagepicker.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.databinding.SceytItemGalleryImageBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemGalleryVideoBinding
import com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders.GalleryImageViewHolder
import com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders.GalleryVideoViewHolder
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.GalleryPickerStyle

class GalleryMediaAdapter(private var clickListener: MediaClickListener) : RecyclerView.Adapter<BaseViewHolder<MediaItem>>() {
    private var currentList = arrayListOf<MediaItem>()

    companion object {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MediaItem> {
        return when (viewType) {
            ViewType.Image.ordinal -> GalleryImageViewHolder(SceytItemGalleryImageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false), clickListener)

            ViewType.Video.ordinal -> GalleryVideoViewHolder(SceytItemGalleryVideoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false), clickListener)

            else -> throw Exception("Unsupported view type")
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<MediaItem>) {
        currentList = data.toArrayList()
        notifyDataSetChanged()
    }

    fun addNewData(data: List<MediaItem>) {
        currentList.addAll(data)
        notifyItemRangeInserted(currentList.size - data.size, data.size)
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is MediaItem.Image -> ViewType.Image.ordinal
            is MediaItem.Video -> ViewType.Video.ordinal
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<MediaItem>, position: Int) {
        holder.bind(currentList[position])
    }

    fun interface MediaClickListener {
        fun onClick(item: MediaItem)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder<MediaItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder<MediaItem>) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    enum class ViewType {
        Image, Video
    }
}