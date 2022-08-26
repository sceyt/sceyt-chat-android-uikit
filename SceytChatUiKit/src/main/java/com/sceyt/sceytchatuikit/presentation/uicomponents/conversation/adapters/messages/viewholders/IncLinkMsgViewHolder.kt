package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.databinding.SceytItemIncLinkMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

class IncLinkMsgViewHolder(
        private val binding: SceytItemIncLinkMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        linkPreview: LinkPreviewHelper,
        private val messageListeners: MessageClickListenersImpl?,
) : BaseLinkMsgViewHolder(linkPreview, binding.root, messageListeners) {

    private lateinit var messageItem: MessageListItem.MessageItem

    init {
        binding.setMessageItemStyle()

        binding.layoutDetails.setOnLongClickListener {
            messageListeners?.onMessageLongClick(it, messageItem)
            return@setOnLongClickListener true
        }

        binding.layoutDetails.setOnClickListener {
            messageListeners?.onLinkClick(it, messageItem)
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        if (item is MessageListItem.MessageItem) {
            messageItem = item
            with(binding) {
                val message = item.message

                if (diff.edited || diff.bodyChanged) {
                    val space = if (message.state == MessageState.Edited) MessagesStyle.INC_EDITED_SPACE else MessagesStyle.INC_DEFAULT_SPACE
                    messageBody.text = HtmlCompat.fromHtml("${message.body} $space", HtmlCompat.FROM_HTML_MODE_LEGACY)
                }

                if (diff.edited || diff.statusChanged) {
                    setMessageStatusAndDateText(message, messageDate)
                    setMessageDateDependAttachments(messageDate, message.files)
                }

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replayCountChanged)
                    setReplayCount(tvReplayCount, toReplayLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.replayContainerChanged)
                    setReplayedMessageContainer(message, binding.viewReplay)

                if (item.message.canShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
                    }

                loadLinkPreview(messageItem, layoutLinkPreview, messageBody)
            }
        }
    }

    private fun SceytItemIncLinkMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
        }
    }
}