package com.sceyt.sceytchatuikit.presentation.root

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.mappers.toTransferData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentDataItem

class AttachmentViewHolderHelper(itemView: View) {
    private var context: Context = itemView.context
    private lateinit var fileItem: AttachmentDataItem
    val isFileItemInitialized get() = this::fileItem.isInitialized

    var listenerKey: String = ""
        private set
    var blurredThumb: Drawable? = null
        private set
    var imageSize: Size? = null
        private set
    var transferData: TransferData? = null


    fun bind(item: AttachmentDataItem) {
        fileItem = item
        blurredThumb = item.blurredThumb?.toDrawable(context.resources)
        imageSize = item.size

        listenerKey = getKey()
        transferData = item.file.toTransferData()
    }

    protected fun getKey(): String {
        if (isFileItemInitialized.not()) return ""
        return fileItem.file.messageTid.toString()
    }

    fun loadThumb(path: String?, imageView: ImageView) {
        Glide.with(context.applicationContext)
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

    fun loadBlurThumb(thumb: Drawable? = blurredThumb, imageView: ImageView) {
        imageView.setImageDrawable(thumb)
    }
}