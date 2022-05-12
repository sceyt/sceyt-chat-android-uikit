package com.sceyt.chat.ui.extensions

import android.content.Context
import android.text.Editable
import androidx.core.text.isDigitsOnly
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.R

fun isAllStringsNotNullOrBlank(vararg strings: Editable?): Boolean {
    for (string in strings) {
        if (string.isNullOrBlank())
            return false
    }
    return true
}

fun isAllStringsIsNumber(vararg strings: Editable?): Boolean {
    for (string in strings) {
        if (string.isNullOrBlank() || !string.isDigitsOnly())
            return false
    }
    return true
}

fun String.filterEmotion(): String {
    val characterFilter = "[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]"
    return replace(characterFilter.toRegex(), "").trim()
}

fun String.onlyLetters() = filter {
    it.isLetter()
}

fun String?.isEqualsVideoOrImage(): Boolean {
    return this == "video" || this == "image"
}

fun Message.getShowBody(context: Context): String {
    return when {
        attachments.isNullOrEmpty() -> body.trim()
        else -> context.getString(R.string.attachment)
    }
}

fun Message.isTextMessage() = attachments.isNullOrEmpty()

fun Message.getAttachmentUrl(): String? {
    if (!attachments.isNullOrEmpty()) {
        attachments[0].apply {
            if (type.isEqualsVideoOrImage())
                return url
        }
    }
    return null
}
