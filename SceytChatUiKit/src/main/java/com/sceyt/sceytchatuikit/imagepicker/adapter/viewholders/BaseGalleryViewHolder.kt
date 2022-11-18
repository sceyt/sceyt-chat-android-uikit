package com.sceyt.sceytchatuikit.imagepicker.adapter.viewholders

import android.content.Context
import android.view.View
import android.widget.Toast
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.imagepicker.adapter.GalleryMediaAdapter.MediaClickListener
import com.sceyt.sceytchatuikit.imagepicker.adapter.MediaItem
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder

abstract class BaseGalleryViewHolder(view: View,
                                     protected val clickListener: MediaClickListener) : BaseViewHolder<MediaItem>(view) {

    protected val context: Context by lazy { view.context }

    protected fun onItemClick(item: MediaItem) {
        if (item.media.isWrongImage) {
            Toast.makeText(context, context.getString(R.string.unsupported_file_format), Toast.LENGTH_SHORT).show()
            return
        }
        item.media.selected = !item.media.selected
        clickListener.onClick(item)
    }
}