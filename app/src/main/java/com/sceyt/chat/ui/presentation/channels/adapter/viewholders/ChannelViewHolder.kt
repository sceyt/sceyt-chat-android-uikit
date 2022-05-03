package com.sceyt.chat.ui.presentation.channels.adapter.viewholders

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.data.models.SceytUiDirectChannel
import com.sceyt.chat.ui.data.models.SceytUiGroupChannel
import com.sceyt.chat.ui.databinding.ItemChannelBinding
import com.sceyt.chat.ui.extencions.getCompatColor
import com.sceyt.chat.ui.extencions.getPresentableName
import com.sceyt.chat.ui.presentation.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.utils.DateTimeUtil

class ChannelViewHolder(private val binding: ItemChannelBinding) : BaseChannelViewHolder(binding.root) {

    init {
        binding.setChannelItemStyle()
    }

    override fun bindViews(item: ChannelListItem) {
        when (item) {
            is ChannelListItem.ChannelItem -> {
                val channel = item.channel
                with(binding) {
                    this.channel = channel
                    val name: String
                    val url: String

                    if (channel.channelType == ChannelTypeEnum.Group) {
                        channel as SceytUiGroupChannel
                        name = channel.subject ?: ""
                        url = channel.avatarUrl ?: ""
                    } else {
                        channel as SceytUiDirectChannel
                        name = channel.peer?.getPresentableName() ?: ""
                        url = channel.peer?.avatarURL ?: ""
                    }
                    avatar.setNameAndImageUrl(name, url)
                    channelTitle.text = name
                    lastMessage.text = getLastMessageTxt(channel.lastMessage)
                    updateDate.text = getDateTxt(channel)
                    setUnreadCount(channel = channel)
                }
            }
            ChannelListItem.LoadingMoreItem -> Unit
        }
    }

    @SuppressLint("SetTextI18n")
    private fun ItemChannelBinding.setUnreadCount(channel: SceytUiChannel) {
        if (channel.unreadMessageCount == 0L) {
            messageCount.visibility = View.GONE
        } else {
            messageCount.visibility = View.VISIBLE
            if (channel.unreadMessageCount > 99L)
                messageCount.text = "99+"
            else
                messageCount.text = channel.unreadMessageCount.toString()
        }
    }

    private fun getLastMessageTxt(message: Message?): String {
        if (message == null) return ""
        return if (message.state == MessageState.Deleted) {
            itemView.context.getString(R.string.message_was_deleted)
        } else {
            val body = if (message.body.isNullOrBlank() && !message.attachments.isNullOrEmpty())
                itemView.context.getString(R.string.attachment) else message.body
            if (!message.incoming) {
                getFormattedYouMessage(body)
            } else body
        }
    }

    private fun getFormattedYouMessage(args: String): String {
        return itemView.resources.getString(R.string.your_last_message).format(args)
    }

    private fun getDateTxt(channel: SceytUiChannel?): String {
        if (channel == null) return ""
        val lastMsgCreatedAt = channel.lastMessage?.createdAt
        return if (lastMsgCreatedAt != null && lastMsgCreatedAt.time != 0L)
            DateTimeUtil.getDateTimeString(lastMsgCreatedAt.time)
        else
            DateTimeUtil.getDateTimeString(channel.updatedAt)
    }

    private fun ItemChannelBinding.setChannelItemStyle() {
        with(root.context) {
            channelTitle.setTextColor(getCompatColor(ChannelStyle.titleColor))
            lastMessage.setTextColor(getCompatColor(ChannelStyle.lastMessageTextColor))
            messageCount.backgroundTintList = ColorStateList.valueOf(getCompatColor(ChannelStyle.unreadCountColor))
        }

        //channelTitle.setTextColor( root.context.getCompatColorNight(R.color.colorFontDark))
    }
}