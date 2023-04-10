package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import android.util.Size
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.SceytItemOutVideoMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.setTextAndDrawableColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class OutVideoMsgViewHolder(
        private val binding: SceytItemOutVideoMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        senderNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, messageListeners, senderNameBuilder = senderNameBuilder, needMediaDataCallback = needMediaDataCallback) {

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                messageListeners?.onMessageClick(it, messageListItem as MessageListItem.MessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
                return@setOnLongClickListener true
            }

            videoViewController.setOnClickListener {
                messageListeners?.onAttachmentClick(it, fileItem)
            }

            videoViewController.setOnLongClickListener {
                messageListeners?.onAttachmentLongClick(it, fileItem)
                return@setOnLongClickListener true
            }

            loadProgress.setOnClickListener {
                messageListeners?.onAttachmentLoaderClick(it, fileItem)
            }
        }
    }


    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)
        fileItem = getFileItem(item as MessageListItem.MessageItem) ?: return
        viewHolderHelper.bind(fileItem)
        setVideoDuration(binding.tvDuration)

        with(binding) {
            val message = item.message
            tvForwarded.isVisible = message.isForwarded

            val body = message.body.trim()
            if (body.isNotBlank()) {
                messageBody.isVisible = true
                setMessageBody(messageBody, message)
            } else messageBody.isVisible = false

            if (!diff.hasDifference()) return

            if (diff.edited || diff.statusChanged)
                setMessageStatusAndDateText(message, messageDate)

            if (diff.replyCountChanged)
                setReplyCount(tvReplyCount, toReplyLine, item)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply)

            if (diff.filesChanged)
                initAttachment(true)

            if (diff.reactionsChanged)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions, binding.layoutDetails)

            if (diff.bodyChanged && !diff.reactionsChanged && recyclerViewReactions != null)
                initWidthsDependReactions(recyclerViewReactions, layoutDetails, message)
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        if (!viewHolderHelper.updateTransferData(data, fileItem)) return

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        val imageView = binding.videoViewController.getImageView()
        when (data.state) {
            TransferState.PendingUpload, TransferState.ErrorUpload, TransferState.PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            TransferState.PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
                binding.videoViewController.showPlayPauseButtons(false)
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }
            TransferState.Downloading -> {
                binding.videoViewController.showPlayPauseButtons(false)
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = imageView)
            }
            TransferState.Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            TransferState.Downloaded -> {
                binding.videoViewController.showPlayPauseButtons(true)
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }
            TransferState.Uploaded -> {
                binding.videoViewController.showPlayPauseButtons(true)
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }
            TransferState.PauseDownload -> {
                binding.videoViewController.showPlayPauseButtons(false)
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }
            TransferState.ErrorDownload -> {
                binding.videoViewController.showPlayPauseButtons(false)
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }
            TransferState.FilePathChanged -> {
                requestThumb()
            }
            TransferState.ThumbLoaded -> {
                viewHolderHelper.drawImageWithBlurredThumb(fileItem.thumbPath, imageView)
            }
        }
    }

    override val fileContainer: View
        get() = binding.videoViewController

    override val loadingProgressView: SceytCircularProgressView
        get() = binding.loadProgress

    override val layoutDetails: ConstraintLayout
        get() = binding.layoutDetails

    override fun getThumbSize() = Size(fileContainer.width, fileContainer.height)

    private fun SceytItemOutVideoMessageBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
            tvForwarded.setTextAndDrawableColor(SceytKitConfig.sceytColorAccent)
        }
    }
}