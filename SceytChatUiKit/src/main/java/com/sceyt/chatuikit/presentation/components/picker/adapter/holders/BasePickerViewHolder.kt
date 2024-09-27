package com.sceyt.chatuikit.presentation.components.picker.adapter.holders

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaAdapter.MediaClickListener
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import com.sceyt.chatuikit.styles.MediaPickerStyle

abstract class BasePickerViewHolder(
        view: View,
        private val style: MediaPickerStyle,
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

    protected fun setGalleryItemCheckedState(image: ImageView, isChecked: Boolean) {
        with(image) {
            if (isChecked) {
                setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
                setImageDrawable(style.checkedStateIcon)
            } else {
                setBackgroundTint(Color.TRANSPARENT)
                setImageDrawable(style.unCheckedStateIcon)
            }
        }
    }
}