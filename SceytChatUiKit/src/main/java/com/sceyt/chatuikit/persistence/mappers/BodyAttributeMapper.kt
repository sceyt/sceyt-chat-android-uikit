package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chatuikit.data.models.messages.SceytBodyAttribute
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyAttributeType
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.format.toStyleType
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention

fun BodyStyleRange.toBodyAttribute(): SceytBodyAttribute {
    return SceytBodyAttribute(style.value, offset, length, null)
}

fun BodyAttribute.toBodyStyleRange(): BodyStyleRange? {
    return BodyStyleRange(offset, length, style = type.toStyleType() ?: return null)
}

fun Mention.toBodyAttribute(): SceytBodyAttribute {
    return SceytBodyAttribute(BodyAttributeType.Mention.value, start, length, recipientId)
}