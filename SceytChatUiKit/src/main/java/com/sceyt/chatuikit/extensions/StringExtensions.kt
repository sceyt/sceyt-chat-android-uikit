package com.sceyt.chatuikit.extensions

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.util.Linkify
import android.util.Base64
import android.util.Patterns
import android.widget.TextView
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import com.google.gson.Gson
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.MarkerType
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
    if (isNullOrBlank()) return false
    for (char in this) {
        when (getDirectionality(char)) {
            DIRECTIONALITY_RIGHT_TO_LEFT,
            DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
            DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE -> return true
        }
    }
    return false
}

fun CharSequence?.getFirstCharIsEmoji(): Pair<CharSequence, Boolean> {
    if (isNullOrBlank()) return "" to false

    // Only use EmojiCompat if it's already loaded to avoid delays
    if (isEmojiCompatReady()) {
        val processed = processEmojiCompat(0, length, 1, EmojiCompat.REPLACE_STRATEGY_ALL)
        if (processed is Spannable) {
            val emojiSpans = processed.getSpans(0, processed.length, EmojiSpan::class.java)
            val firstEmojiSpan = emojiSpans.firstOrNull()
            if (firstEmojiSpan != null) {
                val spanStart = processed.getSpanStart(firstEmojiSpan)
                if (spanStart == 0) {
                    val spanEnd = processed.getSpanEnd(firstEmojiSpan)
                    return processed.subSequence(spanStart, spanEnd) to true
                }
            }
        }
    } else {
        val emojiSequence = extractEmojiSequenceWithoutEmojiCompat()
        if (emojiSequence.isNotEmpty()) {
            return emojiSequence to true
        }
    }

    return take(1) to false
}

/**
 * Extract complete emoji sequence including ZWJ sequences like 😶‍🌫️
 * without using EmojiCompat for better performance
 */
private fun CharSequence.extractEmojiSequenceWithoutEmojiCompat(): CharSequence {
    if (isEmpty()) return ""

    var index: Int
    val maxLength = length

    // Check if starts with emoji
    if (!isLikelyEmoji(this[0])) return ""

    // Handle surrogate pairs
    index = if (Character.isHighSurrogate(this[0]) && maxLength > 1) {
        2 // Skip surrogate pair
    } else {
        1
    }

    // Continue reading ZWJ sequences and variation selectors
    while (index < maxLength) {
        val char = this[index]
        val codePoint = char.code

        when (codePoint) {
            0x200D -> {
                index++
                // Skip the next emoji after ZWJ
                if (index < maxLength) {
                    if (Character.isHighSurrogate(this[index]) && index + 1 < maxLength) {
                        index += 2 // Skip surrogate pair
                    } else {
                        index++
                    }
                }
            }
            // Variation selectors (like ️ in 🌫️)
            in 0xFE00..0xFE0F -> index++
            // Skin tone modifiers
            in 0x1F3FB..0x1F3FF -> index++
            // Stop if not part of emoji sequence
            else -> break
        }
    }

    return take(index)
}

/**
 * Fast emoji detection without EmojiCompat dependency
 */
private fun isLikelyEmoji(char: Char): Boolean {
    val codePoint = char.code
    return when {
        // Basic emoji ranges
        codePoint in 0x1F600..0x1F64F || // Emoticons
                codePoint in 0x1F300..0x1F5FF || // Misc Symbols and Pictographs
                codePoint in 0x1F680..0x1F6FF || // Transport and Map
                codePoint in 0x1F1E0..0x1F1FF || // Regional indicators (flags)
                codePoint in 0x2600..0x26FF ||   // Misc symbols
                codePoint in 0x2700..0x27BF ||   // Dingbats
                codePoint in 0xFE00..0xFE0F ||   // Variation selectors
                codePoint in 0x1F900..0x1F9FF || // Supplemental Symbols and Pictographs
                codePoint in 0x1F018..0x1F270 || // Various symbols
                Character.isHighSurrogate(char) -> true // Potentially part of emoji sequence
        else -> false
    }
}

/**
 * Check if EmojiCompat is ready without blocking
 */
private fun isEmojiCompatReady(): Boolean {
    return try {
        EmojiCompat.get().loadState == EmojiCompat.LOAD_STATE_SUCCEEDED
    } catch (e: Exception) {
        false
    }
}

fun CharSequence?.processEmojiCompat(): CharSequence? {
    return try {
        EmojiCompat.get().process(this)
    } catch (e: Exception) {
        println("EmojiCompat.process: not initialized yet")
        this
    }
}

fun CharSequence?.processEmojiCompat(
        start: Int,
        end: Int,
        maxCount: Int,
        @EmojiCompat.ReplaceStrategy strategy: Int,
): CharSequence? {
    return try {
        EmojiCompat.get().process(this, start, end, maxCount, strategy)
    } catch (e: Exception) {
        println("EmojiCompat.process: not initialized yet")
        this
    }
}

fun CharSequence.notAutoCorrectable(): CharSequence {
    return "\u2068${autoCorrectable()}\u2068"
}

fun CharSequence.autoCorrectable(): String {
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

internal fun String.toDeliveryStatus(): MessageDeliveryStatus? {
    return when (this) {
        MarkerType.Displayed.value -> MessageDeliveryStatus.Displayed
        MarkerType.Received.value -> MessageDeliveryStatus.Received
        else -> null
    }
}

fun String.whitSpace() = plus(" ")

const val empty = ""