package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.util.Size
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import com.google.android.material.imageview.ShapeableImageView
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.extensions.calculateScaleWidthHeight
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.setMargins
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.filetransfer.ThumbData
import com.sceyt.chatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.chatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.chatuikit.presentation.helpers.AttachmentViewHolderHelper
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

abstract class BaseMediaMessageViewHolder(
        val view: View,
        style: MessageItemStyle,
        messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)? = null,
        userNameFormatter: UserNameFormatter?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMsgViewHolder(view, style, messageListeners, displayedListener, userNameFormatter) {
    protected val viewHolderHelper by lazy { AttachmentViewHolderHelper(itemView) }
    protected lateinit var fileItem: FileListItem
    protected var resizedImageSize: Size? = null
    protected open val maxSize by lazy {
        bubbleMaxWidth - dpToPx(4f) //4f is margins
    }
    protected open val minSize get() = maxSize / 3
    protected var isAttachedToWindow = true
    protected var addedLister = false

    @CallSuper
    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        fileItem = getFileItem(item as MessageListItem.MessageItem) ?: return
        fileContainer?.let {
            setImageSize(it)
        }
        viewHolderHelper.bind(fileItem, resizedImageSize)
    }

    protected open fun initAttachment() {
        setListener()

        viewHolderHelper.transferData?.let {
            loadingProgressView.release(it.progressPercent)
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload && it.state != PauseDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }
    }

    protected open fun setImageSize(fileImage: View) {
        val layoutBubble = (layoutBubble as? ConstraintLayout) ?: return
        val size = calculateScaleWidthHeight(maxSize, minSize, imageWidth = fileItem.size?.width
                ?: maxSize,
            imageHeight = fileItem.size?.height ?: maxSize)
        resizedImageSize = size
        val constraintSet = ConstraintSet()
        constraintSet.clone(layoutBubble)
        constraintSet.constrainMinHeight(fileImage.id, size.height)
        constraintSet.constrainMinWidth(fileImage.id, size.width)
        constraintSet.applyTo(layoutBubble)

        val message = requireMessage
        with(fileImage) {
            val defaultMargin = marginLeft
            if (message.isForwarded || message.isReplied || message.shouldShowAvatarAndName || message.body.isNotNullOrBlank()) {
                setMargins(defaultMargin, defaultMargin + dpToPx(4f), defaultMargin, defaultMargin)
            } else setMargins(defaultMargin)
        }
    }

    protected open fun requestThumb() {
        itemView.post {
            if (fileItem.file.filePath.isNullOrBlank()) return@post
            val thumbData = ThumbData(ThumbFor.MessagesLisView.value, getThumbSize())
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem.file, thumbData))
        }
    }

    protected open fun getFileItem(item: MessageListItem.MessageItem): FileListItem? {
        return item.message.files?.find { it.file.type != AttachmentTypeEnum.Link.value() }
    }

    protected open fun setVideoDuration(tvDuration: TextView) {
        with(tvDuration) {
            fileItem.duration?.let {
                text = DateTimeUtil.secondsToTime(it)
                isVisible = true
            } ?: run { isVisible = false }
        }
    }

    protected open fun isValidThumb(thumbData: ThumbData?): Boolean {
        return thumbData?.size == getThumbSize() && thumbData.key == ThumbFor.MessagesLisView.value
    }

    protected open fun setImageTopCorners(fileImage: ShapeableImageView) {
        val message = requireMessage
        val corner = (if (message.isForwarded || message.body.isNotBlank() || message.isReplied) {
            dpToPx(5f)
        } else dpToPx(18f)).toFloat()

        fileImage.setShapeAppearanceModel(
            fileImage.shapeAppearanceModel.toBuilder()
                .setTopLeftCornerSize(corner)
                .setTopRightCornerSize(corner)
                .build()
        )
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
            if (viewHolderHelper.updateTransferData(it, fileItem, ::isValidThumb))
                updateState(it)
        }
    }
}