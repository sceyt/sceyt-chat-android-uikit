package com.sceyt.chatuikit.presentation.components.global_search

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.shared.utils.isGlobalSearchWordStart
import com.sceyt.chatuikit.shared.utils.tokenizeGlobalSearchQuery

/**
 * Applies [highlightColor] spans to every occurrence of each word in [query] that starts
 * at a word boundary in [text]: beginning of string or after an ASCII punctuation/whitespace
 * separator.
 *
 * Mirrors the word-prefix semantics used by [GlobalSearchDao.searchMessages].
 */
internal fun highlightQueryWords(
    text: CharSequence,
    query: String,
    highlightColor: Int,
): CharSequence {
    if (query.isBlank() || text.isBlank()) return text
    val words = tokenizeGlobalSearchQuery(query)
    if (words.isEmpty()) return text
    val spannable = SpannableStringBuilder(text)
    val textLower = text.toString().lowercase()
    for (word in words) {
        val wordLower = word.lowercase()
        var from = 0
        while (from < textLower.length) {
            val start = textLower.indexOf(wordLower, from)
            if (start < 0) break
            if (isGlobalSearchWordStart(textLower, start)) {
                spannable.setSpan(
                    ForegroundColorSpan(highlightColor),
                    start,
                    start + wordLower.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            from = start + 1
        }
    }
    return spannable
}

/**
 * Returns the first index in [text] where [word] begins at a word boundary,
 * or -1 if no such position exists.
 */
internal fun findWordPrefixIndex(text: String, word: String): Int {
    if (word.isEmpty()) return -1

    var from = 0
    while (from < text.length) {
        val idx = text.indexOf(word, from)
        if (idx < 0) return -1
        if (isGlobalSearchWordStart(text, idx)) return idx
        from = idx + 1
    }
    return -1
}
