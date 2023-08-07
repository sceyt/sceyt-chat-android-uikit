package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.util.Size
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.setMargins
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbData
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.sceytchatuikit.presentation.root.AttachmentViewHolderHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil

abstract class BaseMediaMessageViewHolder(
        val view: View,
        messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)? = null,
        senderNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMsgViewHolder(view, messageListeners, displayedListener, senderNameBuilder) {
    protected val viewHolderHelper by lazy { AttachmentViewHolderHelper(itemView) }
    protected lateinit var fileItem: FileListItem
    protected var resizedImageSize: Size? = null
    private val maxSize by lazy {
        bubbleMaxWidth - dpToPx(4f) //4f is margins
    }
    private val minSize = maxSize / 3
    protected var isAttachedToWindow = true
    private var addedLister = false

    @CallSuper
    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)
        fileItem = getFileItem(item as MessageListItem.MessageItem) ?: return
        fileContainer?.let {
            setImageSize(it)
        }
        viewHolderHelper.bind(fileItem, resizedImageSize)
    }

    protected fun initAttachment() {
        setListener()

        viewHolderHelper.transferData?.let {
            loadingProgressView.release(it.progressPercent)
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload && it.state != PauseDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }
    }

    protected fun setImageSize(fileImage: View) {
        val size = calculateScaleWidthHeight(maxSize, minSize, imageWidth = fileItem.size?.width
                ?: maxSize,
            imageHeight = fileItem.size?.height ?: maxSize)
        resizedImageSize = size
        val layoutBubble = (layoutBubble as? ConstraintLayout) ?: return
        val constraintSet = ConstraintSet()
        constraintSet.clone(layoutBubble)
        constraintSet.constrainMinHeight(fileImage.id, size.height)
        constraintSet.constrainMinWidth(fileImage.id, size.width)
        constraintSet.applyTo(layoutBubble)

        val message = fileItem.sceytMessage
        with(fileImage) {
            val defaultMargin = marginLeft
            if (message.isForwarded || message.isReplied || message.canShowAvatarAndName || message.body.isNotNullOrBlank()) {
                setMargins(defaultMargin, defaultMargin + dpToPx(4f), defaultMargin, defaultMargin)
            } else setMargins(defaultMargin)
        }
    }

    protected fun requestThumb() {
        itemView.post {
            if (fileItem.file.filePath.isNullOrBlank()) return@post
            val thumbData = ThumbData(ThumbFor.MessagesLisView.value, getThumbSize())
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem.file, thumbData))
        }
    }

    protected fun getFileItem(item: MessageListItem.MessageItem): FileListItem? {
        return item.message.files?.find { it.file.type != AttachmentTypeEnum.Link.value() }
    }

    protected fun setVideoDuration(tvDuration: TextView) {
        with(tvDuration) {
            fileItem.duration?.let {
                text = DateTimeUtil.secondsToTime(it)
                isVisible = true
            } ?: run { isVisible = false }
        }
    }

    protected fun isValidThumb(thumbData: ThumbData?): Boolean {
        return thumbData?.size == getThumbSize() && thumbData.key == ThumbFor.MessagesLisView.value
    }

    open fun getThumbSize(): Size {
        val with = resizedImageSize?.width ?: fileContainer?.width ?: itemView.width
        val height = resizedImageSize?.height ?: fileContainer?.height ?: itemView.height
        return Size(with, height)
    }

    open val fileContainer: View? = null

    abstract val loadingProgressView: SceytCircularProgressView

    open fun updateState(data: TransferData, isOnBind: Boolean = false) {
        val isTransferring = data.isTransferring()
        if (!isOnBind && !isAttachedToWindow && isTransferring) return
        loadingProgressView.getProgressWithState(data.state, data.progressPercent)
    }

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        isAttachedToWindow = true
        viewHolderHelper.transferData?.let {
            loadingProgressView.getProgressWithState(it.state, it.progressPercent)
        }
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        isAttachedToWindow = false
    }

    private fun setListener() {
        if (addedLister) return
        addedLister = true
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity()) {
            if (viewHolderHelper.updateTransferData(it, fileItem))
                updateState(it)
        }
    }

    private fun calculateScaleWidthHeight(defaultSize: Int, minSize: Int, imageWidth: Int, imageHeight: Int): Size {
        val coefficient = imageWidth.toDouble() / imageHeight.toDouble()
        var scaleWidth = defaultSize
        var scaleHeight = defaultSize

        if (coefficient.isNaN()) {
            return Size(scaleWidth, scaleHeight)
        } else {
            if (coefficient != 1.0) {
                if (imageWidth > imageHeight) {
                    val h = (defaultSize / coefficient).toInt()
                    scaleHeight = if (h >= minSize)
                        h
                    else minSize
                } else {
                    val futureW = (defaultSize * coefficient).toInt()
                    val coefficientWidth = futureW.toDouble() / defaultSize.toDouble()
                    var newDefaultSize = defaultSize

                    // If the width of the image is less than 80% of the default size, then we can increase the default size by 20%
                    if (coefficientWidth <= 0.8)
                        newDefaultSize = (defaultSize * 1.2).toInt()

                    val w = (newDefaultSize * coefficient).toInt()

                    scaleWidth = if (w >= minSize)
                        w
                    else minSize

                    scaleHeight = newDefaultSize
                }
            }
            return Size(scaleWidth, scaleHeight)
        }
    }
}