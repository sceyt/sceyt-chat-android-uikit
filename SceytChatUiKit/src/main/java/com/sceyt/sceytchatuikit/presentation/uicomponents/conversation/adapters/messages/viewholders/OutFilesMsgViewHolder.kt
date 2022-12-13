package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytItemOutFilesMessageBinding
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.findIndexed
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.helpers.RecyclerItemOffsetDecoration

class OutFilesMsgViewHolder(
        private val binding: SceytItemOutFilesMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val viewPoolFiles: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListenersImpl?,
        senderNameBuilder: ((User) -> String)?,
        private val needDownloadCallback: (FileListItem) -> Unit,
) : BaseMsgViewHolder(binding.root, messageListeners, senderNameBuilder = senderNameBuilder) {
    private var filedAdapter: MessageFilesAdapter? = null

    init {
        binding.setMessageItemStyle()

        binding.layoutDetails.setOnLongClickListener {
            messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
            return@setOnLongClickListener true
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message

                val body = message.body.trim()
                if (body.isNotBlank()) {
                    messageBody.isVisible = true
                    messageBody.text = body
                } else messageBody.isVisible = false

                if (diff.edited || diff.statusChanged) {
                    setMessageStatusAndDateText(message, messageDate)
                    setMessageDateDependAttachments(messageDate, message.files)
                }

                if (diff.filesChanged)
                    setFilesAdapter(message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, binding.viewReply)
            }
        }
    }

    private fun setFilesAdapter(item: SceytMessage) {
        val attachments = ArrayList(item.files ?: return)
        with(binding.rvFiles) {
            setHasFixedSize(true)
            if (itemDecorationCount == 0) {
                val offset = dpToPx(4f)
                addItemDecoration(RecyclerItemOffsetDecoration(left = offset, top = offset, right = offset))
            }
            setRecycledViewPool(viewPoolFiles)
            adapter = MessageFilesAdapter(attachments, FilesViewHolderFactory(context = context,
                messageListeners = messageListeners, needDownloadCallback)).also {
                filedAdapter = it
            }
        }
    }

    override fun updateTransfer(data: TransferData) {
        filedAdapter?.getData()?.findIndexed { it.file.tid == data.attachmentTid }?.let {
            it.second.file.fileTransferData = data
            /*  if (data.state == ProgressState.Downloaded)
                  it.second.file.filePath = data.filePath
              if (data.state==ProgressState.Uploaded)
                  it.second.file.url = data.url*/
           /* (binding.rvFiles.findViewHolderForAdapterPosition(it.first) as? BaseFileViewHolder)?.bind(it.second)
                    ?: filedAdapter?.notifyItemChanged(it.first)*/
        }
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        filedAdapter?.onItemDetached()
    }

    private fun SceytItemOutFilesMessageBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
        }
    }
}