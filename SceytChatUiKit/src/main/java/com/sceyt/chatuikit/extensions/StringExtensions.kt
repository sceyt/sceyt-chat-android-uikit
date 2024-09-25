package com.sceyt.chatuikit.extensions

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.util.Linkify
import android.util.Base64
import android.util.Patterns
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import com.google.gson.Gson
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.data.models.messages.MarkerTypeEnum
import java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT
import java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING
import java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
import java.lang.Character.getDirectionality
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
    return !isNullOrBlank()
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

fun CharSequence?.extractLinks(): Array<String> {
    if (this.isNullOrBlank() || isValidEmail()) return emptyArray()
    val links = ArrayList<String>()
    val m = Patterns.WEB_URL.matcher(this)
    while (m.find()) {
        val url = m.group()
        links.add(url)
    }
    return links.toTypedArray()
}

fun CharSequence?.extractLinksWithPositions(): Array<Triple<String, Int, Int>> {
    if (this.isNullOrBlank() || isValidEmail()) return emptyArray()
    val links = ArrayList<Triple<String, Int, Int>>()
    val m = Patterns.WEB_URL.matcher(this)
    while (m.find()) {
        val url = m.group()
        links.add(Triple(url, m.start(), m.end()))
    }
    return links.toTypedArray()
}

fun String?.isValidUrl(context: Context): Boolean {
    this ?: return false
    return Linkify.addLinks(TextView(context).apply { text = this@isValidUrl }, Linkify.WEB_URLS)
}

fun CharSequence?.isValidEmail(): Boolean {
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
    val processed = processEmojiCompat(0, length - 1, 1, EmojiCompat.REPLACE_STRATEGY_ALL)
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

fun CharSequence?.processEmojiCompat(): CharSequence? {
    return try {
        EmojiCompat.get().process(this)
    } catch (e: Exception) {
        println("EmojiCompat.process: not initialized yet")
        this
    }
}

fun CharSequence?.processEmojiCompat(start: Int, end: Int, maxCount: Int, @EmojiCompat.ReplaceStrategy strategy: Int): CharSequence? {
    return try {
        EmojiCompat.get().process(this, start, end, maxCount, strategy)
    } catch (e: Exception) {
        println("EmojiCompat.process: not initialized yet")
        this
    }
}

fun String.notAutoCorrectable(): String {
    return "\u2068${autoCorrectable()}\u2068"
}

fun String.autoCorrectable(): String {
    return replace("\u2068".toRegex(), "")
}

fun CharSequence.removeSpaces(): CharSequence {
    return replace(" ".toRegex(), "")
}

fun String.toSha256(): Long {
    val bytes = toByteArray(StandardCharsets.UTF_8)
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    val bigInt = BigInteger(1, digest)
    return bigInt.toLong()
}

fun Char.isVisuallyEmpty(): Boolean {
    return Character.isWhitespace(this) || hashSetOf('\u200E',  // left-to-right mark
        '\u200F',  // right-to-left mark
        '\u2007',  // figure space
        '\u200B',  // zero-width space
        '\u2800').contains(this) // braille blank
}

fun <T> String?.jsonToObject(clazz: Class<T>): T? {
    return try {
        Gson().fromJson(this, clazz)
    } catch (e: Exception) {
        null
    }
}

internal fun String.toDeliveryStatus(): DeliveryStatus? {
    return when (this) {
        MarkerTypeEnum.Displayed.value() -> DeliveryStatus.Displayed
        MarkerTypeEnum.Received.value() -> DeliveryStatus.Received
        else -> null
    }
}

const val empty = ""