package com.sceyt.chat.ui.presentation.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.UserPresenceStatus
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.ChannelConfig.setChannelItemStyle
import com.sceyt.chat.ui.databinding.ItemChannelBinding
import com.sceyt.chat.ui.extencions.getPresentableName
import java.util.*

class ChannelsAdapter(private var channels: List<Channel>) : RecyclerView.Adapter<ChannelsAdapter.ChannelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        return ChannelViewHolder(ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bindViews(position)
    }

    override fun getItemCount(): Int = channels.size

    fun notifyUpdate(channels: List<Channel>) {


    }

    inner class ChannelViewHolder(private val binding: ItemChannelBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setChannelItemStyle()
        }

        fun bindViews(position: Int) {
            val item = channels[position]
            binding.channel = item

            val name: String
            val url: String
            if (item is GroupChannel) {
                name = item.subject
                url = item.avatarUrl
            } else {
                name = (item as DirectChannel).peer?.getPresentableName() ?: ""
                url = item.peer?.avatarURL ?: ""
            }
            binding.avatar.setNameAndImageUrl(name, url)
            binding.channelTitle.text = name
            binding.lastMessage.text = getLastMessageTxt(item.lastMessage)
            binding.updateDate.text = getDateTxt(item)
            binding.messageStatus.setImageResource(getMessageStatusResId(item.lastMessage))
            binding.setUnreadCount(channel = item)
            binding.setOnlineStatus(channel = item)
        }

        private fun ItemChannelBinding.setUnreadCount(channel: Channel) {
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

        private fun ItemChannelBinding.setOnlineStatus(channel: Channel) {
            onlineStatus.isVisible = (channel is DirectChannel)
                    && channel.peer.presenceStatus == UserPresenceStatus.Online
        }

        private fun getLastMessageTxt(message: Message?): String {
            return if (message != null) {
                if (message.state == MessageState.Deleted) {
                    //itemView.context.getString(R.string.message_was_deleted)
                    "Message was deleted"
                } else {
                    val body = if (message.body.isNullOrBlank() && !message.attachments.isNullOrEmpty())
                    /*itemView.context.getString(R.string.attachment)*/ "atttachemnt" else message.body
                    /*  if (!message.incoming) {
                          getFormattedYouMessage(body)
                      } else*/ body
                }
            } else ""
        }

        /*    private fun getFormattedYouMessage(args: String): String {
                var tmp = youMessage
                if (tmp == null) {
                    tmp = itemView.resources.getString(R.string.your_last_message)
                    youMessage = tmp
                }
                return tmp.format(args)
            }*/

        private fun getDateTxt(channel: Channel?): String {
            return if (channel != null)
                if (channel.lastMessage?.createdAt != null && channel.lastMessage?.createdAt?.time != 0L)
                    getDateTimeString(channel.lastMessage.createdAt.time)
                else
                    getDateTimeString(channel.updatedAt)
            else
                ""
        }

        private fun getDateTimeString(time: Long): String {
            val result: String
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = time
            result = DateFormat.format("HH:mm", cal).toString()
            return result
        }

        private fun getMessageStatusResId(message: Message?): Int {
            return if (message != null && !message.incoming) {
                when (message.deliveryStatus) {
                    DeliveryStatus.Pending -> R.drawable.ic_status_not_sent
                    DeliveryStatus.Sent -> R.drawable.ic_status_on_server
                    DeliveryStatus.Delivered -> R.drawable.ic_status_delivered
                    DeliveryStatus.Read -> R.drawable.ic_status_read
                    else -> {
                        0//it's will set drawable as null
                    }
                }
            } else
                0//it's will set drawable as null
        }
    }
}