package com.sceyt.sceytchatuikit.extensions

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Patterns
import androidx.core.text.isDigitsOnly
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT
import java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING
import java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
import java.lang.Character.getDirectionality
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

fun CharSequence?.setBoldSpan(from: Int, to: Int): SpannableStringBuilder {
    val str = SpannableStringBuilder(this)
    str.setSpan(StyleSpan(Typeface.BOLD), from, to, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return str
}

fun CharSequence?.firstCharToUppercase(): CharSequence? {
    if (this == null || isNullOrBlank()) return this
    return replaceRange(0, 1, first().uppercase())
}

fun String?.toByteArraySafety(): ByteArray? {
    this ?: return null
    return try {
        Base64.decode(this, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

fun String?.extractLinks(): Array<String> {
    if (this.isNullOrBlank() || isValidEmail()) return emptyArray()
    val links = ArrayList<String>()
    val m = Patterns.WEB_URL.matcher(this)
    while (m.find()) {
        val url = m.group()
        links.add(url)
    }
    return links.toTypedArray()
}

fun String?.isValidEmail(): Boolean {
    this ?: return false
    val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
    return emailRegex.matches(this)
}

fun String?.isRtl(): Boolean {
    this ?: return false
    for (char in this) {
        when (getDirectionality(char)) {
            DIRECTIONALITY_RIGHT_TO_LEFT,
            DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
            DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE -> return true

            DIRECTIONALITY_LEFT_TO_RIGHT,
            DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING,
            DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE -> return false
        }
    }
    return false
}

fun String?.getFirstCharIsEmoji(): Pair<CharSequence, Boolean> {
    if (isNullOrBlank()) return Pair("", false)
    val processed = getSafetyEmojiCompat()?.process(this, 0, length - 1, 1, EmojiCompat.REPLACE_STRATEGY_ALL)
    return if (processed is Spannable) {
        val emojiSpans = processed.getSpans(0, processed.length - 1, EmojiSpan::class.java)
        val emojiSpan = emojiSpans.getOrNull(0) ?: Pair(take(1), false)
        val spanStart = processed.getSpanStart(emojiSpan)
        if (spanStart > 0)
            return Pair(take(1), false)

        val spanEnd = processed.getSpanEnd(emojiSpan)
        return Pair(processed.subSequence(spanStart, spanEnd), true)
    } else Pair(take(1), false)
}

fun String.notAutoCorrectable(): String {
    return "\u2068${autoCorrectable()}\u2068"
}

fun String.autoCorrectable(): String {
    return replace("\u2068".toRegex(), "")
}