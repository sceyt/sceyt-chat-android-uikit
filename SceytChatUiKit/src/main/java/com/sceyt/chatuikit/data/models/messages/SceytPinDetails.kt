package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.message.PinDetails.PinType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytPinDetails(
    val isPinned: Boolean,
    val pinnedTill: Long,
    val pinType: PinType,
) : Parcelable