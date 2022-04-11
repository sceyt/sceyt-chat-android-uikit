package com.sceyt.chat.ui.extencions

import android.text.Editable
import androidx.core.text.isDigitsOnly

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


