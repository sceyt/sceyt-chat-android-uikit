package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyAttributeType
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.format.toStyleType

fun BodyStyleRange.toBodyAttribute(): BodyAttribute {
    return BodyAttribute(style.toString(), offset, length, null)
}

fun BodyAttribute.toBodyStyleRange(): BodyStyleRange? {
    return BodyStyleRange(offset, length, style = type.toStyleType() ?: return null)
}

fun Mention.toBodyAttribute(): BodyAttribute {
    return BodyAttribute(BodyAttributeType.Mention.value(), start, length, recipientId)
}