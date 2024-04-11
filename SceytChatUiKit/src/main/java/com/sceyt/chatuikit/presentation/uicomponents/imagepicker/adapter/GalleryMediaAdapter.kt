package com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemGalleryImageBinding
import com.sceyt.chatuikit.databinding.SceytItemGalleryVideoBinding
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.viewholders.GalleryImageViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.viewholders.GalleryVideoViewHolder
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class GalleryMediaAdapter(private var clickListener: MediaClickListener) : RecyclerView.Adapter<BaseViewHolder<MediaItem>>() {
    private var currentList = arrayListOf<MediaItem>()

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