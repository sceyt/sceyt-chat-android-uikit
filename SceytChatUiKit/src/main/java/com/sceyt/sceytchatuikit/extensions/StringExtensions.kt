package com.sceyt.sceytchatuikit.extensions

import android.text.Editable
import androidx.core.text.isDigitsOnly
import java.util.regex.Matcher
import java.util.regex.Pattern

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

fun String?.isLink(): Boolean {
    val regex = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$"

    val p: Pattern = Pattern.compile(regex)
    val m: Matcher = p.matcher(this ?: return false)

    return m.find()
}

fun CharSequence?.isNotNullOrBlank(): Boolean {
    return isNullOrBlank().not()
}