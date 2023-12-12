package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyAttributeType
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.toStyleType

fun BodyStyleRange.toBodyAttribute(): BodyAttribute {
    return BodyAttribute(style.toString(), offset, length, null)
}

fun BodyAttribute.toBodyStyleRange(): BodyStyleRange? {
    return BodyStyleRange(offset, length, style = type.toStyleType() ?: return null)
}

fun Mention.toBodyAttribute(): BodyAttribute {
    return BodyAttribute(BodyAttributeType.Mention.toString(), start, length, recipientId)
}