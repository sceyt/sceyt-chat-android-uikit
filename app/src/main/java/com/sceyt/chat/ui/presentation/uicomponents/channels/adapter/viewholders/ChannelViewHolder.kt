package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.*
import com.sceyt.chat.ui.databinding.SceytItemChannelBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.extensions.getPresentableName
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.utils.DateTimeUtil

class ChannelViewHolder(private val binding: SceytItemChannelBinding,
                        private var listeners: ChannelClickListenersImpl) : BaseViewHolder<ChannelListItem>(binding.root) {

    init {
        binding.setChannelItemStyle()
    }

    override fun bindViews(item: ChannelListItem) {
        when (item) {
            is ChannelListItem.ChannelItem -> {
                val channel = item.channel
                with(binding) {
                    data = channel
                    val name: String
                    val url: String

                    if (channel.channelType.isGroup()) {
                        channel as SceytGroupChannel
                        name = channel.subject ?: ""
                        url = channel.avatarUrl ?: ""
                    } else {
                        channel as SceytDirectChannel
                        name = channel.peer?.getPresentableName() ?: ""
                        url = channel.peer?.avatarURL ?: ""
                    }
                    avatar.setNameAndImageUrl(name, url)
                    channelTitle.text = name
                    setLastMessageTxt(lastMessage, channel.lastMessage)
                    updateDate.setDateText(getDateTxt(channel), false)
                    messageCount.isVisible = false

                    root.setOnClickListener {
                        listeners.onChannelClick(item)
                    }

                    root.setOnLongClickListener {
                        listeners.onChannelLongClick(item)
                        return@setOnLongClickListener true
                    }

                    avatar.setOnClickListener {
                        listeners.onAvatarClick(item)
                    }
                }
            }
            ChannelListItem.LoadingMoreItem -> Unit
        }
    }

    private fun setLastMessageTxt(lastMessage: TextView, message: Message?) {
        if (message == null) return
        if (message.state == MessageState.Deleted) {
            lastMessage.text = itemView.context.getString(R.string.message_was_deleted)
            lastMessage.setTypeface(lastMessage.typeface, Typeface.ITALIC)
        } else {
            val body = if (message.body.isNullOrBlank() && !message.attachments.isNullOrEmpty())
                itemView.context.getString(R.string.attachment) else message.body

            val showText = if (!message.incoming) {
                getFormattedYouMessage(body)
            } else body
            lastMessage.text = showText
            lastMessage.setTypeface(lastMessage.typeface, Typeface.NORMAL)
        }
    }

    private fun getFormattedYouMessage(args: String): String {
        return itemView.resources.getString(R.string.your_last_message).format(args)
    }

    private fun getDateTxt(channel: SceytChannel?): String {
        if (channel == null) return ""
        val lastMsgCreatedAt = channel.lastMessage?.createdAt
        return if (lastMsgCreatedAt != null && lastMsgCreatedAt.time != 0L)
            DateTimeUtil.getDateTimeString(lastMsgCreatedAt.time)
        else
            DateTimeUtil.getDateTimeString(channel.updatedAt)
    }

    private fun SceytItemChannelBinding.setChannelItemStyle() {
        with(root.context) {
            channelTitle.setTextColor(getCompatColorByTheme(ChannelStyle.titleColor))
            lastMessage.setTextColor(getCompatColorByTheme(ChannelStyle.lastMessageTextColor))
            messageCount.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(ChannelStyle.unreadCountColor))
        }
    }
}