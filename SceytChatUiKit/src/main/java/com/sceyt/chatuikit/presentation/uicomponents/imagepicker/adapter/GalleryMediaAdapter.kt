package com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.viewholders.BaseGalleryViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter.viewholders.GalleyMediaItemViewHolderFactory
import com.sceyt.chatuikit.sceytstyles.GalleryPickerStyle

class GalleryMediaAdapter(private val viewHolderFactory: GalleyMediaItemViewHolderFactory,
                          private val style: GalleryPickerStyle
) : RecyclerView.Adapter<BaseGalleryViewHolder>() {
    private var currentList = arrayListOf<MediaItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseGalleryViewHolder {
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

    override fun onBindViewHolder(holder: BaseGalleryViewHolder, position: Int) {
        holder.bind(currentList[position], diff = GalleryMediaItemDiff.DEFAULT)
    }

    override fun onBindViewHolder(holder: BaseGalleryViewHolder, position: Int, payloads: MutableList<Any>) {
        val diff = payloads.find { it is GalleryMediaItemDiff } as? GalleryMediaItemDiff
                ?: GalleryMediaItemDiff.DEFAULT
        holder.bind(item = currentList[position], diff = diff)
    }

    fun interface MediaClickListener {
        fun onClick(item: MediaItem, position: Int)
    }

    override fun onViewAttachedToWindow(holder: BaseGalleryViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseGalleryViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    enum class ViewType {
        Image, Video
    }
}