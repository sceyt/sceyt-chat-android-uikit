package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import android.view.View
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.filetransfer.ThumbData
import com.sceyt.chatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.chatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.chatuikit.presentation.helpers.AttachmentViewHolderHelper
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentDataItem

abstract class BaseFileViewHolder<Item : AttachmentDataItem>(itemView: View,
                                                             private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseViewHolder<Item>(itemView) {
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
            loadingProgressView?.release(it.progressPercent)
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload && it.state != TransferState.PauseDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
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
            if (fileItem.file.filePath.isNullOrBlank()) return@post
            val thumbData = ThumbData(thumbFromEnum.value, getThumbSize())
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem.file, thumbData))
        }
    }

    protected fun isValidThumb(data: ThumbData?): Boolean {
        return getThumbSize() == data?.size && needThumbFor()?.value == data.key
    }

    open fun updateState(data: TransferData, isOnBind: Boolean = false) {
        val isTransferring = data.isTransferring()
        if (!isOnBind && !isAttachedToWindow && isTransferring) return
        loadingProgressView?.getProgressWithState(data.state, data.progressPercent)
    }

    open fun needThumbFor(): ThumbFor? = null

    open fun getThumbSize() = Size(itemView.width, itemView.height)

    protected open val loadingProgressView: SceytCircularProgressView? = null

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        isAttachedToWindow = true
        viewHolderHelper.transferData?.let {
            loadingProgressView?.getProgressWithState(it.state, it.progressPercent)
        }
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        isAttachedToWindow = false
    }
}