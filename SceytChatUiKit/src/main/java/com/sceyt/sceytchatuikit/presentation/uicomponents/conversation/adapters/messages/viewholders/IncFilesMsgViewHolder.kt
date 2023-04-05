package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytItemIncFilesMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.setTextAndDrawableColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class IncFilesMsgViewHolder(
        private val binding: SceytItemIncFilesMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val viewPoolFiles: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
        senderNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseMsgViewHolder(binding.root, messageListeners, displayedListener, senderNameBuilder) {
    private var filedAdapter: MessageFilesAdapter? = null

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
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message
                tvForwarded.isVisible = message.isForwarded

                val body = message.body.trim()
                if (body.isNotBlank()) {
                    messageBody.isVisible = true
                    setMessageBody(messageBody, message)
                } else messageBody.isVisible = false

                if (!diff.hasDifference()) return

                if (diff.edited || diff.statusChanged) {
                    setMessageDateDependAttachments(messageDate, message.files)
                    setMessageStatusAndDateText(message, messageDate)
                }

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.filesChanged)
                    setFilesAdapter(message)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, binding.viewReply)

                if (item.message.canShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
                    }
            }
        }
    }

    private fun setFilesAdapter(message: SceytMessage) {
        val attachments = ArrayList(message.files ?: return)

        initFilesRecyclerView(message, binding.rvFiles)

        if (filedAdapter == null) {
            with(binding.rvFiles) {
                setHasFixedSize(true)

                setRecycledViewPool(viewPoolFiles)
                itemAnimator = null
                adapter = MessageFilesAdapter(attachments, FilesViewHolderFactory(context = context, messageListeners, needMediaDataCallback)).also {
                    filedAdapter = it
                }
            }
        } else filedAdapter?.notifyUpdate(attachments)

    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        filedAdapter?.onItemDetached()
    }

    private fun SceytItemIncFilesMessageBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
            tvUserName.setTextColor(getCompatColorByTheme(MessagesStyle.senderNameTextColor))
            tvForwarded.setTextAndDrawableColor(SceytKitConfig.sceytColorAccent)
        }
    }
}