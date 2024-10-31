package com.sceyt.chatuikit.presentation.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider

class AttachmentViewHolderHelper(itemView: View) {
    private var context: Context = itemView.context
    private lateinit var fileItem: AttachmentDataProvider
    val isFileItemInitialized get() = this::fileItem.isInitialized
    var blurredThumb: Drawable? = null
        private set
    var size: Size? = null
        private set
    var resizedImageSize: Size? = null
        private set

    fun bind(item: AttachmentDataProvider, resizedImageSize: Size? = null) {
        if (isFileItemInitialized && item.thumbPath == null && !fileItem.thumbPath.isNullOrBlank()
                && fileItem.attachment.messageTid == item.attachment.messageTid)
            item.updateThumbPath(fileItem.thumbPath)

        this.resizedImageSize = resizedImageSize
        fileItem = item
        blurredThumb = item.blurredThumb?.toDrawable(context.resources)
        size = item.size
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
        if (!fileItem.attachment.filePath.isNullOrBlank())
            drawImageWithBlurredThumb(fileItem.attachment.filePath, imageView)
        else
            loadBlurThumb(blurredThumb, imageView)
    }

    fun updateTransferData(
            data: TransferData,
            item: AttachmentDataProvider,
            isValidThumb: (thumbData: ThumbData?) -> Boolean,
    ): Boolean {
        if (data.messageTid != item.attachment.messageTid) return false
        if (data.state == TransferState.ThumbLoaded) {
            if (isValidThumb(data.thumbData)) {
                item.updateThumbPath(data.filePath)
            }
        } else {
            item.updateAttachment(item.attachment.getUpdatedWithTransferData(data))
            item.updateTransferData(data)
        }
        return true
    }
}