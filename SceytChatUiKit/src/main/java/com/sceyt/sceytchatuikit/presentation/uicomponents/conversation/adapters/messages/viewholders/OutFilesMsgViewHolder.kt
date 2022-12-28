package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytItemOutFilesMessageBinding
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
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
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
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
                    setMessageDateDependAttachments(messageDate, message.files)
                    setMessageStatusAndDateText(message, messageDate)
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
                val offset = dpToPx(2f)
                addItemDecoration(RecyclerItemOffsetDecoration(left = offset, top = offset, right = offset))
            }
            setRecycledViewPool(viewPoolFiles)
            itemAnimator = null
            adapter = MessageFilesAdapter(attachments, FilesViewHolderFactory(context = context,
                messageListeners = messageListeners, needMediaDataCallback)).also {
                filedAdapter = it
            }
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