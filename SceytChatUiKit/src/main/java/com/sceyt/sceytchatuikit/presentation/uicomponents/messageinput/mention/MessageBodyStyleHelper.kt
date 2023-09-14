package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.text.SpannableString
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.sceytchatuikit.persistence.mappers.toBodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyler

object MessageBodyStyleHelper {

    fun buildWithAttributes(body: String, attribute: List<BodyAttribute>?): SpannableString {
        attribute ?: return SpannableString(body)
        return try {
            BodyStyler.appendStyle(body, attribute.mapNotNull { it.toBodyStyleRange() })
        } catch (e: Exception) {
            e.printStackTrace()
            SpannableString.valueOf(body)
        }
    }
}