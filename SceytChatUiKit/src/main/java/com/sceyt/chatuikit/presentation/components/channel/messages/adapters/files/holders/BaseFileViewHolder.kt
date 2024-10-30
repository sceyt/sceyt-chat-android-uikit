package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders

import android.util.Size
import android.view.View
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.getProgressWithState
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.presentation.helpers.AttachmentViewHolderHelper
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle

abstract class BaseFileViewHolder<Item : AttachmentDataProvider>(
        itemView: View,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseViewHolder<Item>(itemView) {
    protected lateinit var fileItem: Item
    protected val viewHolderHelper by lazy { AttachmentViewHolderHelper(itemView) }
    private var addedLister = false
    protected var isAttachedToWindow = true

    override fun bind(item: Item) {
        fileItem = item
        viewHolderHelper.bind(item)
        initAttachment()
    }

    protected fun initAttachment() {
        setListener()

        viewHolderHelper.transferData?.let {
            loadingProgressViewWithStyle?.first?.release(it.progressPercent)
            updateState(it)
            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload && it.state != TransferState.PauseDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
        }
    }

    private fun setListener() {
        if (addedLister) return
        addedLister = true
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity()) {
            if (viewHolderHelper.updateTransferData(it, fileItem, ::isValidThumb))
                updateState(it)
        }
    }

    protected fun requestThumb() {
        val thumbFromEnum = needThumbFor() ?: return
        itemView.post {
            if (fileItem.attachment.filePath.isNullOrBlank()) return@post
            val thumbData = ThumbData(thumbFromEnum.value, fileItem.attachment.filePath, getThumbSize())
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem.attachment, thumbData))
        }
    }

    protected fun isValidThumb(data: ThumbData?): Boolean {
        return getThumbSize() == data?.size && needThumbFor()?.value == data.key
    }

    open fun updateState(data: TransferData, isOnBind: Boolean = false) {
        val isTransferring = data.isTransferring()
        if (!isOnBind && !isAttachedToWindow && isTransferring) return
        loadingProgressViewWithStyle?.let { (loader, style) ->
            loader.getProgressWithState(data.state, style, data.progressPercent)
        }
    }

    open fun needThumbFor(): ThumbFor? = null

    open fun getThumbSize() = Size(itemView.width, itemView.height)

    protected open val loadingProgressViewWithStyle: Pair<CircularProgressView, MediaLoaderStyle>? = null

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        isAttachedToWindow = true
        viewHolderHelper.transferData?.let {
            loadingProgressViewWithStyle?.let { (loader, style) ->
                loader.getProgressWithState(it.state, style, it.progressPercent)
            }
        }
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        isAttachedToWindow = false
    }
}