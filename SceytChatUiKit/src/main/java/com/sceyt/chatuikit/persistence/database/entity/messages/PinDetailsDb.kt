package com.sceyt.chatuikit.persistence.database.entity.messages

import com.sceyt.chat.models.message.PinDetails.PinType

internal data class PinDetailsDb(
    val isPinned: Boolean,
    val pinnedTill: Long,
    val pinType: PinType
)