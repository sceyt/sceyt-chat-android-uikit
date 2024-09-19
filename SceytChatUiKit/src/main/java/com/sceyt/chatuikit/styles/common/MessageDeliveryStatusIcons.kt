package com.sceyt.chatuikit.styles.common

import android.content.Context
import android.graphics.drawable.Drawable
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.StyleConstants

data class MessageDeliveryStatusIcons(
        val pendingIcon: Drawable?,
        val sentIcon: Drawable?,
        val deliveredIcon: Drawable?,
        val readIcon: Drawable?
) {
    internal class Builder(
            private val context: Context
    ) {
        private var pendingIcon = context.getCompatDrawable(R.drawable.sceyt_ic_status_pending)
        private var sentIcon = context.getCompatDrawable(R.drawable.sceyt_ic_status_sent)
        private var deliveredIcon = context.getCompatDrawable(R.drawable.sceyt_ic_status_delivered)
        private var readIcon = context.getCompatDrawable(R.drawable.sceyt_ic_status_read)

        fun pendingIcon(icon: Drawable) = apply {
            pendingIcon = icon
        }

        fun sentIcon(icon: Drawable) = apply {
            sentIcon = icon
        }

        fun deliveredIcon(icon: Drawable) = apply { deliveredIcon = icon }
        fun readIcon(icon: Drawable) = apply { readIcon = icon }


        fun build() = MessageDeliveryStatusIcons(
            pendingIcon = pendingIcon,
            sentIcon = sentIcon,
            deliveredIcon = deliveredIcon,
            readIcon = readIcon
        )


        private fun Drawable?.applyTint(tintColor: Int): Drawable? {
            return if (tintColor != StyleConstants.UNSET_COLOR)
                this?.mutate()?.apply { setTint(tintColor) }
            else this
        }
    }
}
