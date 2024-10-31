package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.util.Size
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import com.google.android.material.imageview.ShapeableImageView
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.extensions.calculateScaleWidthHeight
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.setMargins
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.getProgressWithState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.presentation.helpers.AttachmentViewHolderHelper
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

abstract class BaseMediaMessageViewHolder(
        view: View,
        private val style: MessageItemStyle,
        messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)? = null,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMessageViewHolder(view, style, messageListeners, displayedListener) {
    protected val viewHolderHelper by lazy { AttachmentViewHolderHelper(itemView) }
    protected lateinit var fileItem: FileListItem
    protected var resizedImageSize: Size? = null
    protected open val maxSize by lazy {
        bubbleMaxWidth - dpToPx(4f) //4f is margins
    }
    protected open val minSize get() = maxSize / 3
    protected var addedLister = false

    @CallSuper
    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        fileItem = getFileItem(item as MessageListItem.MessageItem) ?: return
        fileContainer?.let {
            setImageSize(it)
        }
        viewHolderHelper.bind(fileItem, resizedImageSize)
        setListener()
    }

    @CallSuper
    override fun itemUpdated(item: MessageListItem) {
        super.itemUpdated(item)
        fileItem = getFileItem(messageListItem as MessageListItem.MessageItem) ?: return
    }

    protected open fun initAttachment() {
        fileItem.transferData?.let {
            loadingProgressView.release(it.progressPercent)
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload && it.state != PauseDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
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
        val attachment = fileItem.attachment
        itemView.post {
            if (attachment.filePath.isNullOrBlank()) return@post
            val thumbData = ThumbData(ThumbFor.MessagesLisView.value, attachment.filePath, getThumbSize())
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(attachment, thumbData))
        }
    }

    protected open fun getFileItem(item: MessageListItem.MessageItem): FileListItem? {
        return item.message.files?.find { it.attachment.type != AttachmentTypeEnum.Link.value }
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
        } else dpToPx(17f)).toFloat()

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

    abstract val loadingProgressView: CircularProgressView

    open fun updateState(data: TransferData, isOnBind: Boolean = false) {
        loadingProgressView.getProgressWithState(
            state = data.state,
            style = style.mediaLoaderStyle,
            hideOnThumbLoaded = requireMessage.deliveryStatus != DeliveryStatus.Pending,
            progressPercent = data.progressPercent
        )
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