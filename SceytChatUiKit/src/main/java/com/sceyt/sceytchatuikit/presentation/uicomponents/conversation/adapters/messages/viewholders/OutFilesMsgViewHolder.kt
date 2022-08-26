package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytItemOutFilesMessageBinding
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.helpers.RecyclerItemOffsetDecoration

class OutFilesMsgViewHolder(
        private val binding: SceytItemOutFilesMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val viewPoolFiles: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListenersImpl?,
) : BaseMsgViewHolder(binding.root, messageListeners) {

    private lateinit var messageItem: MessageListItem.MessageItem

    init {
        binding.setMessageItemStyle()

        binding.layoutDetails.setOnLongClickListener {
            messageListeners?.onMessageLongClick(it, messageItem)
            return@setOnLongClickListener true
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        if (item is MessageListItem.MessageItem) {
            messageItem = item
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

                if (diff.replayCountChanged)
                    setReplayCount(tvReplayCount, toReplayLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.replayContainerChanged)
                    setReplayedMessageContainer(message, binding.viewReplay)
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
            adapter = MessageFilesAdapter(attachments, FilesViewHolderFactory(context = itemView.context, messageListeners = messageListeners))
        }
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        (binding.rvFiles.adapter as? MessageFilesAdapter)?.onItemDetached()

    }

    private fun SceytItemOutFilesMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
        }
    }
}