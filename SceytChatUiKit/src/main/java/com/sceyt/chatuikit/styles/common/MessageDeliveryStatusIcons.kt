package com.sceyt.chatuikit.styles.common

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.persistence.lazyVar

/**
 * @property pendingIcon - Icon for pending status, default is [R.drawable.sceyt_ic_status_pending].
 * @property sentIcon - Icon for sent status, default is [R.drawable.sceyt_ic_status_sent].
 * @property receivedIcon - Icon for delivered status, default is [R.drawable.sceyt_ic_status_received].
 * @property displayedIcon - Icon for read status, default is [R.drawable.sceyt_ic_status_displayed].
 * */
data class MessageDeliveryStatusIcons(
        val pendingIcon: Drawable?,
        val sentIcon: Drawable?,
        val receivedIcon: Drawable?,
        val displayedIcon: Drawable?
) {

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray?
    ) {
        private var pendingIcon by lazyVar {
            context.getCompatDrawable(R.drawable.sceyt_ic_status_pending)
                ?.applyTint(context, SceytChatUIKit.theme.iconSecondaryColor)
        }
        private var sentIcon = context.getCompatDrawable(R.drawable.sceyt_ic_status_sent)
            ?.applyTint(context, SceytChatUIKit.theme.iconSecondaryColor)

        private var receivedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_status_received)
            ?.applyTint(context, SceytChatUIKit.theme.iconSecondaryColor)

        private var displayedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_status_displayed)
            ?.applyTint(context, SceytChatUIKit.theme.accentColor)

        fun setPendingIconFromStyle(@StyleableRes index: Int) = apply {
            typedArray?.getDrawable(index)?.let {
                pendingIcon = it
            } ?: pendingIcon
        }

        fun setSentIconFromStyle(@StyleableRes index: Int) = apply {
            typedArray?.getDrawable(index)?.let {
                sentIcon = it
            } ?: sentIcon
        }

        fun setReceivedIconIconFromStyle(@StyleableRes index: Int) = apply {
            typedArray?.getDrawable(index)?.let {
                receivedIcon = it
            } ?: receivedIcon
        }

        fun setDisplayedIconFromStyle(@StyleableRes index: Int) = apply {
            typedArray?.getDrawable(index)?.let {
                displayedIcon = it
            } ?: displayedIcon
        }

        fun build() = MessageDeliveryStatusIcons(
            pendingIcon = pendingIcon,
            sentIcon = sentIcon,
            receivedIcon = receivedIcon,
            displayedIcon = displayedIcon
        )
    }
}
