package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sceyt.chat.ui.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle
import com.sceyt.chat.ui.shared.helpers.LinkPreviewHelper

class OutLinkMsgViewHolder(
        private val binding: SceytItemOutLinkMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
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
                messageBody.text = message.body.trim()

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.replayCountChanged)
                    setReplayCount(tvReplayCount, toReplayLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (diff.replayContainerChanged)
                    setReplayedMessageContainer(message, viewReplay)

                layoutDetails.setOnLongClickListener {
                    messageListeners?.onMessageLongClick(it, item)
                    return@setOnLongClickListener true
                }

                LinkPreviewHelper().getPreview(message.id, message.body, successListener = {
                    with(layoutLinkPreview) {
                        if (it.imageUrl.isNullOrBlank().not()) {
                            Glide.with(itemView.context).load(it.imageUrl).into(previewImage)
                            previewImage.isVisible = true
                        } else previewImage.isVisible = false

                        tvLinkTitle.text = it.title
                        tvLinkDesc.text = it.description
                        root.isVisible = true
                    }
                })
            }
        }
    }

    private fun SceytItemOutLinkMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
        }
    }
}