package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.push.PushData

open class DefaultNotificationBodyFormatter : Formatter<PushData> {

    override fun format(context: Context, from: PushData): CharSequence {
        return when (from.type) {
            NotificationType.ChannelMessage -> from.message.body
            NotificationType.MessageReaction -> {
                "${context.getString(R.string.sceyt_reacted)} ${from.reaction?.key} ${context.getString(R.string.sceyt_to)} \"${from.message.body}\""
            }
        }
    }
}