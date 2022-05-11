package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.databinding.SceytUiItemIncFilesMessageBinding
import com.sceyt.chat.ui.databinding.SceytUiItemOutFilesMessageBinding
import com.sceyt.chat.ui.extensions.dpToPx
import com.sceyt.chat.ui.extensions.isEqualsVideoOrImage
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.utils.RecyclerItemOffsetDecoration

class OutFilesMsgViewHolder(
        private val binding: SceytUiItemOutFilesMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
) : BaseMsgViewHolder(binding.root) {

    override fun bindViews(item: MessageListItem) {
        when (item) {
            is MessageListItem.MessageItem -> {
                with(binding) {
                    val message = item.message
                    this.message = message

                    setReplayCount(layoutDetails, tvReplayCount, toReplayLine, message.replyCount)
                    setOrUpdateReactions(message.reactionScores, rvReactions, viewPool)
                    setMessageDay(message.createdAt, message.showDate, messageDay)
                    setMessageDateText(message.createdAt, messageDate, message.state == MessageState.Edited)

                    setFilesAdapter(message)
                }
            }
            MessageListItem.LoadingMoreItem -> return
        }
    }

    private fun setFilesAdapter(item: SceytUiMessage) {
       // if (item.attachments?.lastOrNull()?.type.isEqualsVideoOrImage()) {
            binding.messageDate.setHighlighted(item.attachments?.lastOrNull()?.type.isEqualsVideoOrImage())
       // }
        with(binding.rvFiles) {
            setHasFixedSize(true)
            if (itemDecorationCount == 0) {
                val offset = dpToPx(4f)
                addItemDecoration(RecyclerItemOffsetDecoration(left = offset, top = offset, right = offset))
            }
            setRecycledViewPool(viewPool)
            adapter = MessageFilesAdapter(ArrayList(item.attachments!!.map {
                when (it.type) {
                    "image" -> FileListItem.Image(it)
                    "video" -> FileListItem.Video(it)
                    else -> FileListItem.File(it)
                }
            }), FilesViewHolderFactory(context = itemView.context))
        }
    }
}