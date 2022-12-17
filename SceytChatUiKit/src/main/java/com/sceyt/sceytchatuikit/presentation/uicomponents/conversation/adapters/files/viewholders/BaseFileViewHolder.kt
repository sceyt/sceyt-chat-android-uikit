package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.isNull
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.mappers.getThumbByBytesAndSize
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import jp.wasabeef.glide.transformations.BlurTransformation


abstract class BaseFileViewHolder(itemView: View) : BaseViewHolder<FileListItem>(itemView) {
    protected lateinit var fileItem: FileListItem
    val isFileItemInitialized get() = this::fileItem.isInitialized
    protected val context: Context by lazy { itemView.context }
    protected var listenerKey: String = ""
    protected var transferData: TransferData? = null
    protected var thumb: ByteArray? = null
    protected var imageWidth: Int? = null
    protected var imageHeight: Int? = null

    override fun bind(item: FileListItem) {
        fileItem = item
        if (transferData != null && transferData!!.messageTid == item.sceytMessage.tid) return
        fileItem.file.metadata.getThumbByBytesAndSize()?.let {
            thumb = it.second
            imageWidth = it.first?.width
            imageHeight = it.first?.height
        }

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

    fun loadImage(path: String?, imageView: ImageView) {
        Glide.with(itemView.context.applicationContext)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(imageView.width, imageView.height)
            .into(imageView)
    }

    fun loadBlurImageBytes(bytes: ByteArray?, imageView: ImageView) {
        Glide.with(itemView.context.applicationContext)
            .load(bytes)
            .transform(BlurTransformation())
            .into(imageView)
    }
}