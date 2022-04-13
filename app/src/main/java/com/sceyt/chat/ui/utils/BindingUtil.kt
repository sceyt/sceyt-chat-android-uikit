package com.sceyt.chat.ui.utils

import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.UserPresenceStatus
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.data.models.SceytUiDirectChannel

object BindingUtil {

    @BindingAdapter("bind:status", "bind:incoming")
    @JvmStatic
    fun setMessageStatusIcon(imageView: ImageView, status: DeliveryStatus?, incoming: Boolean?) {
        if (status == null || incoming == true) return
        val iconResId = when (status) {
            DeliveryStatus.Pending -> R.drawable.ic_status_not_sent
            DeliveryStatus.Sent -> R.drawable.ic_status_on_server
            DeliveryStatus.Delivered -> R.drawable.ic_status_delivered
            DeliveryStatus.Read -> R.drawable.ic_status_read
            else -> 0
        }
        imageView.setImageResource(iconResId)
    }

    @BindingAdapter("bind:visibleIf")
    @JvmStatic
    fun visibleIf(anyView: View, show: Boolean) {
        anyView.visibility = if (show) View.VISIBLE else View.GONE
    }

    @BindingAdapter("bind:channel")
    @JvmStatic
    fun setOnlineStatus(view: View, channel: SceytUiChannel?) {
        view.isVisible = (channel?.channelType == ChannelTypeEnum.Direc)
                && (channel as? SceytUiDirectChannel)?.peer?.presenceStatus == UserPresenceStatus.Online
    }
}