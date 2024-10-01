package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytItemOutVideoMessageBinding
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.formatters.UserNameFormatter
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class OutVideoMsgViewHolder(
        private val binding: SceytItemOutVideoMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        userNameFormatter: UserNameFormatter?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, style, messageListeners, userNameFormatter = userNameFormatter, needMediaDataCallback = needMediaDataCallback) {

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                messageListeners?.onMessageClick(it, requireMessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, requireMessageItem)
                return@setOnLongClickListener true
            }

            messageBody.doOnLongClick {
                messageListeners?.onMessageLongClick(it, requireMessageItem)
            }

            messageBody.doOnClickWhenNoLink {
                messageListeners?.onMessageClick(it, requireMessageItem)
            }

            imageThumb.setOnClickListener {
                messageListeners?.onAttachmentClick(it, fileItem, requireMessage)
            }

            imageThumb.setOnLongClickListener {
                messageListeners?.onAttachmentLongClick(it, fileItem, requireMessage)
                return@setOnLongClickListener true
            }

            loadProgress.setOnClickListener {
                messageListeners?.onAttachmentLoaderClick(it, fileItem, requireMessage)
            }
        }
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)

        with(binding) {
            val message = (item as MessageListItem.MessageItem).message
            tvForwarded.isVisible = message.isForwarded
            setVideoDuration(tvDuration)

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

            if (diff.filesChanged) {
                initAttachment()
                setImageTopCorners(imageThumb)
            }

            if (diff.reactionsChanged || diff.edited)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (diff.bodyChanged && !diff.reactionsChanged && recyclerViewReactions != null)
                initWidthsDependReactions(recyclerViewReactions, layoutDetails)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply)
        }
    }

    private fun setFileLoadProgress(data: TransferData) {
        with(binding.tvLoadSize) {
            if (data.state == Preparing) {
                text = context.getString(R.string.sceyt_preparing)
                isVisible = true
                return
            }

            if (data.isTransferring()) {
                val title = "${data.fileLoadedSize} / ${data.fileTotalSize}"
                text = title
                isVisible = true
            }
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        val imageView = binding.imageThumb

        when (data.state) {
            Downloaded, Uploaded -> {
                binding.playPauseItem.isVisible = true
                binding.tvLoadSize.isVisible = false
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }

            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
                binding.tvLoadSize.isVisible = false
                binding.playPauseItem.isVisible = false
            }

            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
                binding.playPauseItem.isVisible = false
                binding.tvLoadSize.isVisible = false
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }

            Downloading -> {
                binding.playPauseItem.isVisible = false
                setFileLoadProgress(data)
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = imageView)
            }

            Uploading, Preparing -> {
                binding.playPauseItem.isVisible = false
                setFileLoadProgress(data)
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }

            WaitingToUpload -> {
                binding.playPauseItem.isVisible = false
                binding.tvLoadSize.isVisible = false
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }

            PauseDownload -> {
                binding.playPauseItem.isVisible = false
                binding.tvLoadSize.isVisible = false
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }

            ErrorDownload -> {
                binding.playPauseItem.isVisible = false
                binding.tvLoadSize.isVisible = false
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }

            FilePathChanged -> {
                if (fileItem.thumbPath.isNullOrBlank())
                    requestThumb()
            }

            ThumbLoaded -> {
                if (isValidThumb(data.thumbData))
                    viewHolderHelper.drawImageWithBlurredThumb(fileItem.thumbPath, imageView)
            }
        }
    }

    override val fileContainer: View
        get() = binding.imageThumb

    override val loadingProgressView: CircularProgressView
        get() = binding.loadProgress

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, true)

    override val selectMessageView get() = binding.selectView

     override val incoming: Boolean
        get() = false

    private fun SceytItemOutVideoMessageBinding.setMessageItemStyle() {
        style.videoDurationTextStyle.apply(tvDuration)
        playPauseItem.setImageDrawable(style.videoPlayIcon)
        playPauseItem.setBackgroundTint(style.onOverlayColor)
        tvDuration.setDrawableStart(style.videoIcon)
        tvDuration.setBackgroundTint(style.onOverlayColor)
        style.overlayMediaLoaderStyle.apply(loadProgress)
        applyCommonStyle(
            layoutDetails = layoutDetails,
            tvForwarded = tvForwarded,
            messageBody = messageBody,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine
        )
    }
}