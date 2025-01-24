package com.sceyt.chatuikit.push

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.notifications.NotificationType
import kotlinx.parcelize.Parcelize

@Parcelize
data class PushData(
        val type: NotificationType,
        val channel: SceytChannel,
        val message: SceytMessage,
        val user: SceytUser,
        val reaction: SceytReaction?
) : Parcelable