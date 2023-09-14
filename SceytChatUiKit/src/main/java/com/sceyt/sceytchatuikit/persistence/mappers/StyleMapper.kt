package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.toStyleType

fun BodyStyleRange.toBodyAttribute(): BodyAttribute {
    return BodyAttribute(style.toString(), offset, length, null)
}

fun BodyAttribute.toBodyStyleRange(): BodyStyleRange? {
    return BodyStyleRange(offset, length, style = type.toStyleType() ?: return null)
}