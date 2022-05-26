package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.databinding.SceytItemIncFilesMessageBinding
import com.sceyt.chat.ui.extensions.dpToPx
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle
import com.sceyt.chat.ui.utils.RecyclerItemOffsetDecoration

class IncFilesMsgViewHolder(
        private val binding: SceytItemIncFilesMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val viewPoolFiles: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListenersImpl,
) : BaseMsgViewHolder(binding.root, messageListeners) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bindViews(item: MessageListItem) {
        when (item) {
            is MessageListItem.MessageItem -> {
                with(binding) {
                    val message = item.message
                    this.message = message

                    setReplayCount(tvReplayCount, toReplayLine, item)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)
                    setMessageDay(message.createdAt, message.showDate, messageDay)
                    setMessageDateText(message.createdAt, messageDate, message.state == MessageState.Edited)
                    setReplayedMessageContainer(message, binding.viewReplay)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)
                    setFilesAdapter(message)

                    layoutDetails.setOnLongClickListener {
                        messageListeners.onMessageLongClick(it, item)
                        return@setOnLongClickListener true
                    }

                    if (item.message.canShowAvatarAndName)
                        avatar.setOnClickListener {
                            messageListeners.onAvatarClick(it, item)
                        }
                }
            }
            MessageListItem.LoadingMoreItem -> return
        }
    }

    private fun setFilesAdapter(item: SceytMessage) {
        val attachments = ArrayList(item.files ?: return)
        binding.messageDate.apply {
            val needHighlight = attachments.lastOrNull() is FileListItem.Image
                    || attachments.lastOrNull() is FileListItem.Video
            setHighlighted(needHighlight)
            val marginEndBottom = if (needHighlight) Pair(20, 20) else Pair(marginEnd, marginBottom)
            (layoutParams as ViewGroup.MarginLayoutParams).apply {
                setMargins(0, marginTop, 0, marginEndBottom.second)
                marginEnd = marginEndBottom.first
            }
        }

        with(binding.rvFiles) {
            setHasFixedSize(true)
            if (itemDecorationCount == 0) {
                val offset = dpToPx(4f)
                addItemDecoration(RecyclerItemOffsetDecoration(left = offset, top = offset, right = offset))
            }
            setRecycledViewPool(viewPoolFiles)
            adapter = MessageFilesAdapter(attachments, FilesViewHolderFactory(context = itemView.context, messageListeners))
        }
    }

    private fun SceytItemIncFilesMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
        }
    }
}