package com.sceyt.chatuikit.presentation.components.global_search

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val RED = 0xFFFF0000.toInt()

@RunWith(RobolectricTestRunner::class)
class SearchQueryHighlightTest {

    // region findWordPrefixIndex

    @Test
    fun findWordPrefixIndex_wordAtStartOfString() {
        assertThat(findWordPrefixIndex("hello world", "hello")).isEqualTo(0)
    }

    @Test
    fun findWordPrefixIndex_wordAfterSpace() {
        assertThat(findWordPrefixIndex("hello world", "world")).isEqualTo(6)
    }

    @Test
    fun findWordPrefixIndex_wordAfterNewline() {
        assertThat(findWordPrefixIndex("hello\nworld", "world")).isEqualTo(6)
    }

    @Test
    fun findWordPrefixIndex_midWordSuffixNotMatched() {
        assertThat(findWordPrefixIndex("hello world", "ello")).isEqualTo(-1)
    }

    @Test
    fun findWordPrefixIndex_midWordSuffixAfterNewlineNotMatched() {
        assertThat(findWordPrefixIndex("hello\nworld", "orld")).isEqualTo(-1)
    }

    @Test
    fun findWordPrefixIndex_wordNotPresentReturnsNegative() {
        assertThat(findWordPrefixIndex("hello world", "xyz")).isEqualTo(-1)
    }

    @Test
    fun findWordPrefixIndex_emptyTextReturnsNegative() {
        assertThat(findWordPrefixIndex("", "hello")).isEqualTo(-1)
    }

    @Test
    fun findWordPrefixIndex_skipsNonBoundaryOccurrenceAndFindsLaterBoundary() {
        // "world" is a suffix of "underworld" (not a word start), but appears again at a word boundary
        assertThat(findWordPrefixIndex("underworld world", "world")).isEqualTo(11)
    }

    @Test
    fun findWordPrefixIndex_caseInsensitiveInput_matchesLowercasedWord() {
        // The function operates on pre-lowercased text; verify the boundary logic holds
        assertThat(findWordPrefixIndex("hello world", "wor")).isEqualTo(6)
    }

    @Test
    fun findWordPrefixIndex_multipleNewlines_findsFirstWordBoundary() {
        assertThat(findWordPrefixIndex("a\nb\nc", "b")).isEqualTo(2)
    }

    // endregion

    // region highlightQueryWords — span positions

    @Test
    fun highlightQueryWords_singleWordAtStart_setsSpanOnFullWord() {
        val result = highlightQueryWords("hello world", "hello", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(5)
    }

    @Test
    fun highlightQueryWords_singleWordAfterSpace_setsSpanAtCorrectPosition() {
        val result = highlightQueryWords("hello world", "world", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(6)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(11)
    }

    @Test
    fun highlightQueryWords_wordAfterNewline_setsSpanAtCorrectPosition() {
        val result = highlightQueryWords("hello\nworld", "world", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(6)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(11)
    }

    @Test
    fun highlightQueryWords_midWordQuery_noSpanApplied() {
        val result = highlightQueryWords("hello world", "ello", RED) as Spanned
        assertThat(result.getSpans(0, result.length, ForegroundColorSpan::class.java)).isEmpty()
    }

    @Test
    fun highlightQueryWords_midWordSuffixAfterNewline_noSpanApplied() {
        val result = highlightQueryWords("hello\nworld", "orld", RED) as Spanned
        assertThat(result.getSpans(0, result.length, ForegroundColorSpan::class.java)).isEmpty()
    }

    @Test
    fun highlightQueryWords_multiWord_eachWordGetsOwnSpan() {
        val result = highlightQueryWords("hello world", "hello world", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(2)
    }

    @Test
    fun highlightQueryWords_sameWordAppearsAtTwoWordBoundaries_bothSpanned() {
        // "hi" at position 0 and position 7 (both word starts)
        val result = highlightQueryWords("hi bye hi", "hi", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(2)
        val starts = spans.map { result.getSpanStart(it) }.sorted()
        assertThat(starts).containsExactly(0, 7).inOrder()
    }

    @Test
    fun highlightQueryWords_wordAppearsOnlyAsSuffixElsewhere_onlyBoundaryOccurrenceSpanned() {
        // "world" is a suffix of "underworld" (no span) and then at word start (span)
        val result = highlightQueryWords("underworld world", "world", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(11)
    }

    @Test
    fun highlightQueryWords_caseInsensitiveMatch_spanApplied() {
        val result = highlightQueryWords("Hello World", "hello", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(5)
    }

    @Test
    fun highlightQueryWords_blankQuery_returnsTextUnchangedWithNoSpans() {
        val result = highlightQueryWords("hello world", "   ", RED)
        assertThat(result.toString()).isEqualTo("hello world")
        assertThat((result as? Spanned)?.getSpans(0, result.length, ForegroundColorSpan::class.java)).isNull()
    }

    @Test
    fun highlightQueryWords_leadingAndTrailingSpacesInQuery_treatedAsWords() {
        val result = highlightQueryWords("hello world", "  hello  ", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
    }

    @Test
    fun highlightQueryWords_multipleNewlinesInBody_allNewlinePrefixWordsSpanned() {
        val result = highlightQueryWords("one\ntwo\nthree", "two three", RED) as Spanned
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertThat(spans.size).isEqualTo(2)
    }

    // endregion
}
