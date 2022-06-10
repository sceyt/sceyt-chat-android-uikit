package com.sceyt.chat.ui.presentation.common

import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.getCompatDrawable
import com.sceyt.chat.ui.presentation.customviews.SceytDateStatusView

fun SceytMessage?.setMessageDateAndStatusIcon(dateStatusView: SceytDateStatusView, dateText: String, edited: Boolean) {
    if (this?.deliveryStatus == null || incoming) {
        dateStatusView.setDateAndStatusIcon(dateText, null, edited)
        return
    }
    val iconResId = when (deliveryStatus) {
        DeliveryStatus.Pending -> R.drawable.sceyt_ic_status_not_sent
        DeliveryStatus.Sent -> R.drawable.sceyt_ic_status_on_server
        DeliveryStatus.Delivered -> R.drawable.sceyt_ic_status_delivered
        DeliveryStatus.Read -> R.drawable.sceyt_ic_status_read
        DeliveryStatus.Failed -> R.drawable.sceyt_ic_status_faild
        else -> null
    }
    iconResId?.let {
        dateStatusView.setDateAndStatusIcon(dateText, dateStatusView.context.getCompatDrawable(it), edited)
        dateStatusView.isVisible = true
    }
}