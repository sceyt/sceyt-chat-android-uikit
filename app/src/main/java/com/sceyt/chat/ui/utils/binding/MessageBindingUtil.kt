package com.sceyt.chat.ui.utils.binding

import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.getCompatDrawable
import com.sceyt.chat.ui.presentation.customviews.SceytDateStatusView
import com.sceyt.chat.ui.presentation.customviews.SceytVideoControllerView

object MessageBindingUtil {

    @BindingAdapter("bind:showPlayPauseButton")
    @JvmStatic
    fun showPlayPauseButton(view: SceytVideoControllerView, show: Boolean) {
        view.showPlayPauseButtons(show)
    }

    @BindingAdapter("bind:status", "bind:incoming")
    @JvmStatic
    fun setMessageStatusIcon(dateStatusView: SceytDateStatusView, status: DeliveryStatus?, incoming: Boolean?) {
        if (status == null || incoming == true) {
            dateStatusView.isVisible = false
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
}