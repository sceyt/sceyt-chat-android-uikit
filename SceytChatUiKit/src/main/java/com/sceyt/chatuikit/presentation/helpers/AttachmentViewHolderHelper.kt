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
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.extensions.isFileNotFound
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider

class AttachmentViewHolderHelper(itemView: View) {
    private var context: Context = itemView.context
    private lateinit var fileItem: AttachmentDataProvider
    val isFileItemInitialized get() = this::fileItem.isInitialized
    private val optionalFileItem get() = if (isFileItemInitialized) fileItem else null
    var blurredThumb: Drawable? = null
        private set
    var size: Size? = null
        private set
    var resizedImageSize: Size? = null
        private set
    private var lastInvalidatedThumbKey: String? = null

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
        val attachment = fileItem.attachment
        val invalidatedThumbKey = "${attachment.messageTid}_${attachment.filePath}_$path"
        if (lastInvalidatedThumbKey == invalidatedThumbKey) return
        lastInvalidatedThumbKey = invalidatedThumbKey
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
        val messageTid = optionalFileItem?.attachment?.messageTid
        val onThumbLoaded = {
            if (isFileItemInitialized && fileItem.thumbPath == path
                && fileItem.attachment.messageTid == messageTid
            )
                lastInvalidatedThumbKey = null
        }
        val listener = glideRequestListener<Drawable>(
            onLoadFailed = { e ->
                logLoadFailed(messageTid, path, e)
                onLoadFailed?.invoke(e)
            },
            onResourceReady = { _, _, _, _, _ ->
                onThumbLoaded()
                onResourceReady?.invoke()
            }
        )
        Glide.with(context.applicationContext)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(blurredThumb)
            .override(width, height)
            .listener(listener)
            .into(imageView)
    }

    private fun logLoadFailed(messageTid: Long?, path: String?, e: GlideException?) {
        SceytLog.w(
            TAG, "Glide couldn't load image for messageTid: ${messageTid}, path:$path," +
                    " fileNotFound:${e.isFileNotFound()}", e
        )
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
