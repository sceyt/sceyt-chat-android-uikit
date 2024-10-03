package com.sceyt.chatuikit.presentation.components.picker.adapter.holders

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaAdapter.MediaClickListener
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem

abstract class BasePickerViewHolder(
        view: View,
        private val clickListener: MediaClickListener
) : RecyclerView.ViewHolder(view) {

    protected val context: Context by lazy { view.context }

    abstract fun bind(item: MediaItem, diff: GalleryMediaItemDiff)
    open fun onViewDetachedFromWindow() {}
    open fun onViewAttachedToWindow() {}

    protected fun onItemClick(item: MediaItem) {
        if (item.media.isWrong) {
            Toast.makeText(context, context.getString(R.string.sceyt_this_unsupported_file_format), Toast.LENGTH_SHORT).show()
            return
        }
        clickListener.onClick(item, bindingAdapterPosition)
    }
}