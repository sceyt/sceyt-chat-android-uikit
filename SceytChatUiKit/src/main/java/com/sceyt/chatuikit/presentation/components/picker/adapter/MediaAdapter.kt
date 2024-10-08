package com.sceyt.chatuikit.presentation.components.picker.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.components.picker.adapter.holders.BasePickerViewHolder
import com.sceyt.chatuikit.presentation.components.picker.adapter.holders.MediaPickerItemViewHolderFactory
import com.sceyt.chatuikit.styles.media_picker.MediaPickerStyle

class MediaAdapter(
        private val viewHolderFactory: MediaPickerItemViewHolderFactory,
        private val style: MediaPickerStyle
) : RecyclerView.Adapter<BasePickerViewHolder>() {
    private var currentList = arrayListOf<MediaItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasePickerViewHolder {
        return viewHolderFactory.onCreateViewHolder(parent, viewType)
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(currentList[position])
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

    override fun onBindViewHolder(holder: BasePickerViewHolder, position: Int) {
        holder.bind(currentList[position], diff = GalleryMediaItemDiff.DEFAULT)
    }

    override fun onBindViewHolder(holder: BasePickerViewHolder, position: Int, payloads: MutableList<Any>) {
        val diff = payloads.find { it is GalleryMediaItemDiff } as? GalleryMediaItemDiff
                ?: GalleryMediaItemDiff.DEFAULT
        holder.bind(item = currentList[position], diff = diff)
    }

    fun interface MediaClickListener {
        fun onClick(item: MediaItem, position: Int)
    }

    override fun onViewAttachedToWindow(holder: BasePickerViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BasePickerViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    enum class ViewType {
        Image, Video
    }
}