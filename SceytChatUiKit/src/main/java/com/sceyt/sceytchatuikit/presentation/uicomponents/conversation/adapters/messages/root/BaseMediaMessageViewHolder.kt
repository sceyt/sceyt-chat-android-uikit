package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginLeft
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.setMargins
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.sceytchatuikit.presentation.root.AttachmentViewHolderHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners

abstract class BaseMediaMessageViewHolder(
        val view: View,
        messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)? = null,
        senderNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMsgViewHolder(view, messageListeners, displayedListener, senderNameBuilder) {

    protected val viewHolderHelper by lazy { AttachmentViewHolderHelper(itemView) }
    protected lateinit var fileItem: FileListItem
    private val maxSize by lazy { context.resources.getDimensionPixelSize(R.dimen.bodyMaxWidth) }
    private val minSize = 300

    protected fun initFileMessage() {
        setListener()

        loadingProgressView.release(fileItem.file.progressPercent)
        viewHolderHelper.transferData?.let {
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }

        if (fileItem.thumbPath.isNullOrBlank())
            requestThumb()

        setImageSize()
    }

    private fun setImageSize() {
        calculateScaleWidthHeight(maxSize, minSize, imageWidth = fileItem.size?.width ?: maxSize,
            imageHeight = fileItem.size?.height ?: maxSize) { width, height ->
            val constraintSet = ConstraintSet()
            constraintSet.clone(layoutDetails)
            constraintSet.constrainMinHeight(fileContainer.id, height)
            constraintSet.constrainMinWidth(fileContainer.id, width)
            constraintSet.applyTo(layoutDetails)

            val message = fileItem.sceytMessage
            with(fileContainer) {
                val defaultMargin = marginLeft
                if (message.isForwarded || message.isReplied || message.canShowAvatarAndName || message.body.isNotNullOrBlank()) {
                    setMargins(defaultMargin, defaultMargin + dpToPx(4f), defaultMargin, defaultMargin)
                } else setMargins(defaultMargin)
            }
        }
    }

    protected fun requestThumb() {
        itemView.post {
            if (fileItem.file.filePath.isNullOrBlank()) return@post
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem.file, getThumbSize()))
        }
    }

    protected fun getFileItem(item: MessageListItem.MessageItem): FileListItem? {
        return item.message.files?.find { it.file.type != AttachmentTypeEnum.Link.value() }
    }

    open fun getThumbSize() = Size(itemView.width, itemView.height)

    abstract val fileContainer: ImageView

    abstract val loadingProgressView: SceytCircularProgressView

    abstract val layoutDetails: ConstraintLayout

    abstract fun updateState(data: TransferData, isOnBind: Boolean = false)

    private fun setListener() {
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity(), ::updateState)
    }

    private fun calculateScaleWidthHeight(defaultSize: Int, minSize: Int, imageWidth: Int, imageHeight: Int, result: (scaleWidth: Int, scaleHeight: Int) -> Unit) {
        val coefficient = imageWidth.toDouble() / imageHeight.toDouble()
        var scaleWidth = defaultSize
        var scaleHeight = defaultSize

        if (coefficient.isNaN()) {
            result.invoke((scaleWidth * 0.8).toInt(), scaleHeight)
            return
        } else {
            if (coefficient != 1.0) {
                if (imageWidth > imageHeight) {
                    val h = (defaultSize / coefficient).toInt()
                    scaleHeight = if (h >= minSize)
                        h
                    else minSize
                } else {
                    val w = (defaultSize * coefficient).toInt()
                    scaleWidth = if (w >= minSize)
                        w
                    else minSize
                }
            }
            result.invoke(scaleWidth, scaleHeight)
        }
    }
}