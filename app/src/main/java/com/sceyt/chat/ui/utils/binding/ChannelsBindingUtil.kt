package com.sceyt.chat.ui.utils.binding

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.getCompatDrawable
import com.sceyt.chat.ui.extensions.getString
import com.sceyt.chat.ui.presentation.customviews.SceytDateStatusView

object ChannelsBindingUtil {

    @BindingAdapter("bind:channel")
    @JvmStatic
    fun setOnlineStatus(view: View, channel: SceytChannel?) {
        view.isVisible = (channel?.channelType == ChannelTypeEnum.Direct)
                && (channel as? SceytDirectChannel)?.peer?.presence?.state == PresenceState.Online
    }


    @BindingAdapter("bind:setLastMessageStatusIcon")
    @JvmStatic
    fun setLastMessageStatusIcon(dateStatusView: SceytDateStatusView, message: SceytMessage?) {
        if (message?.status == null || message.incoming) {
            dateStatusView.setStatusIcon(null)
            return
        }
        val iconResId = when (message.status) {
            DeliveryStatus.Pending -> R.drawable.sceyt_ic_status_not_sent
            DeliveryStatus.Sent -> R.drawable.sceyt_ic_status_on_server
            DeliveryStatus.Delivered -> R.drawable.sceyt_ic_status_delivered
            DeliveryStatus.Read -> R.drawable.sceyt_ic_status_read
            DeliveryStatus.Failed -> R.drawable.sceyt_ic_status_faild
            else -> null
        }
        iconResId?.let {
            dateStatusView.setStatusIcon(dateStatusView.context.getCompatDrawable(it))
            dateStatusView.isVisible = true
        }
    }

    @BindingAdapter("bind:setUnreadCount")
    @JvmStatic
    @SuppressLint("SetTextI18n")
    fun setUnreadCount(textView: TextView, unreadCount: Long?) {
        if (unreadCount == null) {
            textView.isVisible = false
            return
        }
        if (unreadCount == 0L) {
            textView.isVisible = false
        } else {
            textView.isVisible = true
            if (unreadCount > 99L)
                textView.text = "99+"
            else
                textView.text = unreadCount.toString()
        }
    }


    @BindingAdapter("bind:setChannelLastMessageText")
    @JvmStatic
    fun setChannelLastMessageText(textView: TextView, message: SceytMessage?) {
        if (message == null) {
            textView.text = ""
            return
        }
        if (message.state == MessageState.Deleted) {
            textView.text = textView.context.getString(R.string.message_was_deleted)
            textView.setTypeface(null, Typeface.ITALIC)
        } else {
            val body = if (message.body.isBlank() && !message.attachments.isNullOrEmpty())
                textView.context.getString(R.string.attachment) else message.body

            val showText = if (!message.incoming) {
                textView.getString(R.string.your_last_message).format(body.trim())
            } else body.trim()
            textView.text = showText
            textView.setTypeface(null, Typeface.NORMAL)
        }
    }
}