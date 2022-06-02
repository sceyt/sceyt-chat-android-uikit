package com.sceyt.chat.ui.utils.binding

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.extensions.getCompatDrawable
import com.sceyt.chat.ui.presentation.customviews.SceytDateStatusView

object ChannelsBindingUtil {

    @BindingAdapter("bind:channel")
    @JvmStatic
    fun setOnlineStatus(view: View, channel: SceytChannel?) {
        view.isVisible = (channel?.channelType == ChannelTypeEnum.Direct)
                && (channel as? SceytDirectChannel)?.peer?.presence?.state == PresenceState.Online
    }

    @BindingAdapter("bind:status", "bind:incoming")
    @JvmStatic
    fun setMessageStatusIcon(imageView: ImageView, status: DeliveryStatus?, incoming: Boolean?) {
        if (status == null || incoming == true) {
            imageView.isVisible = false
            return
        }
        val iconResId = when (status) {
            DeliveryStatus.Pending -> R.drawable.sceyt_ic_status_not_sent
            DeliveryStatus.Sent -> R.drawable.sceyt_ic_status_on_server
            DeliveryStatus.Delivered -> R.drawable.sceyt_ic_status_delivered
            DeliveryStatus.Read -> R.drawable.sceyt_ic_status_read
            else -> null
        }
        iconResId?.let {
            imageView.setImageResource(it)
            imageView.isVisible = true
        }
    }

    @BindingAdapter("bind:status", "bind:incoming")
    @JvmStatic
    fun setMessageStatusIcon(dateStatusView: SceytDateStatusView, status: DeliveryStatus?, incoming: Boolean?) {
        if (status == null || incoming == true) {
            dateStatusView.setStatusIcon(null)
            return
        }
        val iconResId = when (status) {
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
    fun setUnreadCount(textView: TextView, channel: SceytChannel?) {
        if (channel == null) {
            textView.isVisible = false
            return
        }
        if (channel.unreadMessageCount == 0L) {
            textView.isVisible = false
        } else {
            textView.isVisible = true
            if (channel.unreadMessageCount > 99L)
                textView.text = "99+"
            else
                textView.text = channel.unreadMessageCount.toString()
        }
    }
}