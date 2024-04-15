package com.sceyt.chatuikit.presentation.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.persistence.filetransfer.ThumbData
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentDataItem

class AttachmentViewHolderHelper(itemView: View) {
    private var context: Context = itemView.context
    private lateinit var fileItem: AttachmentDataItem
    val isFileItemInitialized get() = this::fileItem.isInitialized
    var blurredThumb: Drawable? = null
        private set
    var size: Size? = null
        private set
    var resizedImageSize: Size? = null
        private set
    var transferData: TransferData? = null
        private set


    fun bind(item: AttachmentDataItem, resizedImageSize: Size? = null) {
        if (isFileItemInitialized && item.thumbPath == null && !fileItem.thumbPath.isNullOrBlank()
                && fileItem.file.messageTid == item.file.messageTid)
            item.thumbPath = fileItem.thumbPath

        this.resizedImageSize = resizedImageSize
        fileItem = item
        blurredThumb = item.blurredThumb?.toDrawable(context.resources)
        size = item.size
        transferData = item.file.toTransferData()
    }

    fun drawImageWithBlurredThumb(path: String?, imageView: ImageView) {
        val width = resizedImageSize?.width ?: imageView.width
        val height = resizedImageSize?.height ?: imageView.height
        Glide.with(context.applicationContext)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(blurredThumb)
            .override(width, height)
            .into(imageView)
    }

    fun drawThumbOrRequest(imageView: ImageView, requestThumb: () -> Unit) {
        if (isFileItemInitialized.not()) return
        if (!fileItem.thumbPath.isNullOrBlank())
            drawImageWithBlurredThumb(fileItem.thumbPath, imageView)
        else {
            loadBlurThumb(blurredThumb, imageView)
            requestThumb()
        }
    }

    fun loadBlurThumb(thumb: Drawable? = blurredThumb, imageView: ImageView) {
        imageView.setImageDrawable(thumb)
    }

    fun drawOriginalFile(imageView: ImageView) {
        if (isFileItemInitialized.not()) return
        if (!fileItem.file.filePath.isNullOrBlank())
            drawImageWithBlurredThumb(fileItem.file.filePath, imageView)
        else
            loadBlurThumb(blurredThumb, imageView)
    }

    fun updateTransferData(data: TransferData, item: AttachmentDataItem, isValidThumb: (thumbData: ThumbData?) -> Boolean): Boolean {
        if (isFileItemInitialized.not() || (data.messageTid != item.file.messageTid)) return false
        if (data.state == TransferState.ThumbLoaded) {
            if (isValidThumb(data.thumbData))
                fileItem.thumbPath = data.filePath
        } else {
            fileItem.file.updateWithTransferData(data)
            transferData = data
        }
        return true
    }
}