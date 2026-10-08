package com.sceyt.chatuikit.presentation.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.extensions.isFileNotFound
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
    private var lastInvalidatedThumbPath: String? = null

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
        onResourceReady: (() -> Unit)? = null
    ) {
        loadImage(path, imageView, onResourceReady, null)
    }

    fun drawThumbOrRequest(
        imageView: ImageView,
        requestThumb: () -> Unit,
        fallback: Drawable? = null,
    ) {
        if (isFileItemInitialized.not()) return
        val thumbPath = fileItem.thumbPath
        if (!thumbPath.isNullOrBlank()) {
            val onLoadFailed: (GlideException?) -> Unit = { e ->
                if (e.isFileNotFound()) invalidateThumb(thumbPath, requestThumb)
            }
            loadImage(thumbPath, imageView, null, onLoadFailed)
        } else {
            loadBlurThumb(blurredThumb ?: fallback, imageView)
            requestThumb()
        }
    }

    internal fun invalidateThumb(path: String, requestThumb: () -> Unit) {
        if (isFileItemInitialized.not() || fileItem.thumbPath != path) return
        if (lastInvalidatedThumbPath == path) return
        lastInvalidatedThumbPath = path
        fileItem.updateThumbPath(null)
        requestThumb()
    }

    private fun loadImage(
        path: String?,
        imageView: ImageView,
        onResourceReady: (() -> Unit)?,
        onLoadFailed: ((GlideException?) -> Unit)?,
    ) {
        val width = resizedImageSize?.width ?: imageView.width
        val height = resizedImageSize?.height ?: imageView.height
        val listener = glideRequestListener<Drawable>(
            onLoadFailed = { onLoadFailed?.invoke(it) },
            onResourceReady = { _, _, _, _, _ -> onResourceReady?.invoke() }
        )
        Glide.with(context.applicationContext)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(blurredThumb)
            .override(width, height)
            .listener(listener)
            .into(imageView)
    }

    fun loadBlurThumb(thumb: Drawable? = blurredThumb, imageView: ImageView) {
        imageView.setImageDrawable(thumb)
    }

    fun drawOriginalFile(imageView: ImageView, onResourceReady: (() -> Unit)? = null) {
        if (isFileItemInitialized.not()) return
        if (!fileItem.attachment.filePath.isNullOrBlank())
            drawImageWithBlurredThumb(fileItem.attachment.filePath, imageView, onResourceReady)
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
