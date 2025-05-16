package com.sceyt.chatuikit.data.managers.channel.event

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelTypingEventData(
        val channel: SceytChannel,
        val user: SceytUser,
        val typing: Boolean
) : Parcelable