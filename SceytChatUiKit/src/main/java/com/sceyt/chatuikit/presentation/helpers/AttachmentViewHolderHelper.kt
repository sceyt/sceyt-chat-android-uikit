package com.sceyt.chatuikit.presentation.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
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
            && fileItem.attachment.messageTid == item.attachment.messageTid
        )
            item.updateThumbPath(fileItem.thumbPath)

        this.resizedImageSize = resizedImageSize
        fileItem = item
        blurredThumb = item.blurredThumb?.toDrawable(context.resources)
        size = item.size
    }

    fun drawImageWithBlurredThumb(
        path: String?,
        imageView: ImageView,
        placeholder: Drawable? = blurredThumb,
        onResourceReady: (() -> Unit)? = null
    ) {
        val width = (resizedImageSize?.width ?: imageView.width).takeIf { it > 0 }
            ?: Target.SIZE_ORIGINAL
        val height = (resizedImageSize?.height ?: imageView.height).takeIf { it > 0 }
            ?: Target.SIZE_ORIGINAL
        Glide.with(context.applicationContext)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(placeholder)
            .override(width, height)
            .listener(glideRequestListener<Drawable>(onResourceReady = { _, _, _, _, _ ->
                onResourceReady?.invoke()
            }))
            .into(imageView)
    }

    fun drawThumbOrRequest(
        imageView: ImageView,
        requestThumb: () -> Unit,
        fallback: Drawable? = null,
    ) {
        if (isFileItemInitialized.not()) return
        if (!fileItem.thumbPath.isNullOrBlank())
            drawImageWithBlurredThumb(fileItem.thumbPath, imageView)
        else {
            loadBlurThumb(blurredThumb ?: fallback, imageView)
            requestThumb()
        }
    }

    /**
     * Draws the already loaded thumb if it exists, otherwise the blurred thumb from the metadata.
     * Used when the file itself is not loaded yet.
     */
    fun loadBlurThumb(thumb: Drawable? = blurredThumb, imageView: ImageView) {
        if (isFileItemInitialized && !fileItem.thumbPath.isNullOrBlank())
            drawImageWithBlurredThumb(fileItem.thumbPath, imageView, thumb)
        else imageView.setImageDrawable(thumb)
    }

    fun drawOriginalFile(imageView: ImageView, onResourceReady: (() -> Unit)? = null) {
        if (isFileItemInitialized.not()) return
        if (!fileItem.attachment.filePath.isNullOrBlank())
            drawImageWithBlurredThumb(
                path = fileItem.attachment.filePath,
                imageView = imageView,
                onResourceReady = onResourceReady
            )
        else
            loadBlurThumb(blurredThumb, imageView)
    }

    fun updateTransferData(
        data: TransferData,
        item: AttachmentDataProvider,
        isValidThumb: (thumbData: ThumbData?) -> Boolean,
    ): Boolean {
        if (!AttachmentTransferStateStore.isTransferDataForAttachment(data, item.attachment))
            return false

        if (data.state == TransferState.ThumbLoaded) {
            if (!isValidThumb(data.thumbData)) return false
            item.updateThumbPath(data.filePath)
        } else {
            val latestData = AttachmentTransferStateStore.getTransferData(item.attachment) ?: data
            item.updateAttachment(AttachmentTransferStateStore.getUpdatedAttachment(item.attachment, latestData))
            item.updateTransferData(latestData)
        }
        return true
    }
}
