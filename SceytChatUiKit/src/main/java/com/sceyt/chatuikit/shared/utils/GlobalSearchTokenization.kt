package com.sceyt.chatuikit.shared.utils

private const val ASCII_PUNCTUATION = """!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~"""

private val SQL_SEPARATOR_CHARS = charArrayOf(
    ' ',
    '\t',
    '\n',
    '\r',
    '\u000B',
    '\u000C',
) + ASCII_PUNCTUATION.toCharArray()

internal val GLOBAL_SEARCH_SQL_SEPARATOR_CHARS: CharArray =
    SQL_SEPARATOR_CHARS.copyOf()

internal fun tokenizeGlobalSearchQuery(query: String): List<String> {
    if (query.isBlank()) return emptyList()

    val tokens = mutableListOf<String>()
    val current = StringBuilder()

    query.forEach { char ->
        if (isGlobalSearchTokenSeparator(char)) {
            if (current.isNotEmpty()) {
                tokens += current.toString()
                current.setLength(0)
            }
        } else {
            current.append(char)
        }
    }

    if (current.isNotEmpty()) {
        tokens += current.toString()
    }

    return tokens
}

internal fun isGlobalSearchWordStart(text: CharSequence, index: Int): Boolean {
    if (index <= 0) return true
    return isGlobalSearchTokenSeparator(text[index - 1])
}

internal fun isGlobalSearchTokenSeparator(char: Char): Boolean =
    isAsciiWhitespace(char) || isAsciiPunctuation(char)

private fun isAsciiWhitespace(char: Char) = when (char) {
    ' ', '\t', '\n', '\r', '\u000B', '\u000C' -> true
    else -> false
}

private fun isAsciiPunctuation(char: Char): Boolean =
    char.code in 0x21..0x7E && !char.isLetterOrDigit()
