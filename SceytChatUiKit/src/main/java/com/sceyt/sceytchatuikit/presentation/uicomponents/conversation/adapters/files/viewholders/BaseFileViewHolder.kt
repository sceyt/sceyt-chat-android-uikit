package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.isNull
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem


abstract class BaseFileViewHolder(itemView: View) : BaseViewHolder<FileListItem>(itemView) {
    protected lateinit var fileItem: FileListItem
    val isFileItemInitialized get() = this::fileItem.isInitialized
    protected val context: Context by lazy { itemView.context }
    protected var listenerKey: String = ""
    protected var transferData: TransferData? = null
    protected var blurredThumb: Drawable? = null
    protected var imageSize: Size? = null

    override fun bind(item: FileListItem) {
        fileItem = item
        if (transferData != null && transferData!!.messageTid == item.sceytMessage.tid) return
        blurredThumb = item.blurredThumb?.toDrawable(context.resources)
        imageSize = item.size

        item.file.transferState?.let {
            val attachment = item.file
            Log.i(TAG, "${attachment.transferState}  ${attachment.progressPercent}")
            transferData = TransferData(
                messageTid = item.sceytMessage.tid,
                attachmentTid = attachment.tid,
                progressPercent = attachment.progressPercent ?: 0f,
                state = it,
                filePath = attachment.filePath,
                url = attachment.url)
        } ?: run { transferData = null }
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

    protected fun getSize(): Size {
        return Size(itemView.width, itemView.height)
    }

    fun loadBlurThumb(thumb: Drawable?, imageView: ImageView) {
        imageView.setImageDrawable(thumb)
    }
}