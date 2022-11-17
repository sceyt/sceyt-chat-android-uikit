package com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.imagepicker.adapter.GalleryMediaAdapter.MediaClickListener
import com.sceyt.sceytchatuikit.imagepicker.adapter.MediaItem

abstract class BaseGalleryViewHolder(view: View,
                                     protected val clickListener: MediaClickListener) : RecyclerView.ViewHolder(view) {

    protected val context: Context by lazy { view.context }
    abstract fun bind(item: MediaItem)

    protected fun onItemClick(item: MediaItem) {
        if (item.media.isWrongImage) {
            Toast.makeText(context, "This file format is not supported", Toast.LENGTH_SHORT).show()
            return
        }
        item.media.selected = !item.media.selected
        clickListener.onClick(item)
    }
}