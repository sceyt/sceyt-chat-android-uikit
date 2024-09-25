package com.sceyt.chatuikit.data.managers.channel.event

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelTypingEventData(
        val channel: SceytChannel,
        val member: SceytMember,
        val typing: Boolean
) : Parcelable