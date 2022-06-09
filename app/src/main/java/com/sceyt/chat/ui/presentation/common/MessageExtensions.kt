package com.sceyt.chat.ui.presentation.common

import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.getCompatDrawable
import com.sceyt.chat.ui.presentation.customviews.SceytDateStatusView

fun SceytMessage?.setLastMessageStatusIcon(dateStatusView: SceytDateStatusView) {
    if (this?.status == null || incoming) {
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