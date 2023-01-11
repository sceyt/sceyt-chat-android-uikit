package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.extensions.isNull
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.mappers.toTransferData
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem

abstract class BaseFileViewHolder(itemView: View) : BaseViewHolder<FileListItem>(itemView) {
    protected lateinit var fileItem: FileListItem
    val isFileItemInitialized get() = this::fileItem.isInitialized
    protected var listenerKey: String = ""
    protected var transferData: TransferData? = null
    protected var blurredThumb: Drawable? = null
    protected var imageSize: Size? = null

    override fun bind(item: FileListItem) {
        fileItem = item
        if (transferData != null && transferData!!.messageTid == item.sceytMessage.tid) return
        blurredThumb = item.blurredThumb?.toDrawable(context.resources)
        imageSize = item.size

        transferData = item.file.toTransferData()
    }

    protected fun getKey(): String {
        if (isFileItemInitialized.not()) return ""
        val data = fileItem.file
        val key: String = if (data.tid.isNull() || data.tid == 0L) {
            data.url.toString()
        } else {
            data.tid.toString()
        }
        return key
    }

    fun loadThumb(path: String?, imageView: ImageView) {
        Glide.with(itemView.context.applicationContext)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(blurredThumb)
            .override(imageView.width, imageView.height)
            .into(imageView)
    }

    fun drawThumbOrRequest(imageView: ImageView, requestThumb: () -> Unit) {
        if (isFileItemInitialized.not()) return
        if (fileItem.thumbPath != null)
            loadThumb(fileItem.thumbPath, imageView)
        else {
            loadBlurThumb(blurredThumb, imageView)
            requestThumb()
        }
    }

    open fun getThumbSize() = Size(itemView.width / 2, itemView.height)

    fun loadBlurThumb(thumb: Drawable?, imageView: ImageView) {
        imageView.setImageDrawable(thumb)
    }
}