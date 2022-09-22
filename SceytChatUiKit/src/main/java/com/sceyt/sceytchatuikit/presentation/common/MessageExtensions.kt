package com.sceyt.sceytchatuikit.presentation.common

import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.sceytchatuikit.sceytconfigs.ChannelStyle

fun SceytMessage?.setMessageDateAndStatusIcon(dateStatusView: SceytDateStatusView, dateText: String, edited: Boolean) {
    if (this?.deliveryStatus == null || incoming) {
        dateStatusView.setDateAndStatusIcon(dateText, null, edited)
        return
    }
    val iconResId = when (deliveryStatus) {
        DeliveryStatus.Pending -> ChannelStyle.indicatorPendingIcon
        DeliveryStatus.Sent -> ChannelStyle.indicatorSentIcon
        DeliveryStatus.Delivered -> ChannelStyle.indicatorDeliveredIcon
        DeliveryStatus.Read -> ChannelStyle.indicatorReadIcon
        DeliveryStatus.Failed -> R.drawable.sceyt_ic_status_faild
        else -> null
    }
    iconResId?.let {
        dateStatusView.setDateAndStatusIcon(dateText, dateStatusView.context.getCompatDrawable(it), edited)
        dateStatusView.isVisible = true
    }
}