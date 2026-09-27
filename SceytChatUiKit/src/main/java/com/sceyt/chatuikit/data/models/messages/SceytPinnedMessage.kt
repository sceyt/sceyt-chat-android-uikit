package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.message.PinDetails.PinType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytPinnedMessage(
    val id: Long,
    val channelId: Long,
    val messageId: Long,
    val messageTid: Long,
    val scope: PinType,
    val pinnedAt: Long?,
    val pinnedUntil: Long?,
    val pinnedBy: SceytUser?,
    val message: SceytMessage,
) : Parcelable