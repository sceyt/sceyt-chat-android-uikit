package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.text.Annotation
import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionValidatorWatcher.MentionValidator
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQuery
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQueryChangedListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQueryReplacement

class MentionSupportEditText : AppCompatEditText {

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    private var mentionValidatorWatcher: MentionValidatorWatcher = MentionValidatorWatcher()
    private var cursorPositionChangedListener: CursorPositionChangedListener? = null
    private var inlineQueryChangedListener: InlineQueryChangedListener? = null

    private fun initialize() {
        addTextChangedListener(mentionValidatorWatcher)
        addTextChangedListener(MentionDeleter())
        addTextChangedListener(ComposeTextStyleWatcher(context))

        doAfterTextChanged {
            onInputTextChanged(it ?: return@doAfterTextChanged)
        }
    }

    override fun onSelectionChanged(selectionStart: Int, selectionEnd: Int) {
        super.onSelectionChanged(selectionStart, selectionEnd)
        text?.let {
            val selectionChanged = changeSelectionForPartialMentions(it, selectionStart, selectionEnd)
            if (selectionChanged)
                return

            if (selectionStart != selectionEnd)
                clearInlineQuery()
        }

        cursorPositionChangedListener?.onCursorPositionChanged(selectionStart, selectionEnd)
    }

    fun setCursorPositionChangedListener(listener: CursorPositionChangedListener?) {
        cursorPositionChangedListener = listener
    }

    fun setInlineQueryChangedListener(listener: InlineQueryChangedListener?) {
        inlineQueryChangedListener = listener
    }

    fun setMentionValidator(mentionValidator: MentionValidator?) {
        mentionValidatorWatcher.setMentionValidator(mentionValidator)
    }

    fun hasMentions(): Boolean {
        val text = text
        return if (text != null) {
            MentionAnnotation.getMentionAnnotations(text).isNotEmpty()
        } else false
    }

    val mentions: List<Mention>
        get() = MentionAnnotation.getMentionsFromAnnotations(text)

    private fun changeSelectionForPartialMentions(spanned: Spanned, selectionStart: Int, selectionEnd: Int): Boolean {
        val annotations = spanned.getSpans(0, spanned.length, Annotation::class.java)
        for (annotation in annotations) {
            if (MentionAnnotation.isMentionAnnotation(annotation)) {
                val spanStart = spanned.getSpanStart(annotation)
                val spanEnd = spanned.getSpanEnd(annotation)
                val startInMention = selectionStart in (spanStart + 1) until spanEnd
                val endInMention = selectionEnd in (spanStart + 1) until spanEnd
                if (startInMention || endInMention) {
                    if (selectionStart == selectionEnd) {
                        setSelection(spanEnd, spanEnd)
                    } else {
                        val newStart = if (startInMention) spanStart else selectionStart
                        val newEnd = if (endInMention) spanEnd else selectionEnd
                        setSelection(newStart, newEnd)
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun onInputTextChanged(text: Editable) {
        if (enoughToFilter(text)) {
            performFiltering(text)
        } else
            clearInlineQuery()
    }

    private fun performFiltering(text: CharSequence) {
        val end = selectionEnd
        val queryStart = findQueryStart(text, end, false) ?: return
        val start = queryStart.index
        val query = text.subSequence(start, end).toString()
        if (inlineQueryChangedListener != null) {
            if (queryStart.isMentionQuery) {
                inlineQueryChangedListener?.onQueryChanged(InlineQuery.Mention(query))
            }
        }
    }

    private fun clearInlineQuery() {
        inlineQueryChangedListener?.clearQuery()
    }

    private fun enoughToFilter(text: CharSequence): Boolean {
        val end = selectionEnd
        return if (end < 0) {
            false
        } else (findQueryStart(text, end, false)?.index ?: -1) != -1
    }

    fun replaceTextWithMention(displayName: String, recipientId: String) {
        replaceText(createReplacementToken(displayName, recipientId), false)
    }

    fun replaceText(replacement: InlineQueryReplacement) {
        replaceText(replacement.toCharSequence(context), replacement.keywordSearch)
    }

    private fun replaceText(replacement: CharSequence, keywordReplacement: Boolean) {
        val editable = SpannableStringBuilder(text ?: return)
        try {
            clearComposingText()
            val end = selectionEnd
            val start = (findQueryStart(editable, end, keywordReplacement)?.index
                    ?: return) - if (keywordReplacement) 0 else 1

            if (start < 0) return
            editable.replace(start, end, "")
            editable.insert(start, replacement)
            text = editable
            setSelection(start + replacement.length)
        } catch (_: Exception) {
            Log.i("ComposeText", "Failed to replace text with mention.")
        }
    }

    private fun createReplacementToken(text: CharSequence, recipientId: String): CharSequence {
        val builder = SpannableStringBuilder().append(MentionDeleter.MENTION_STARTER)
        if (text is Spanned) {
            val spannableString = SpannableString("$text ")
            TextUtils.copySpansFrom(text, 0, text.length, Any::class.java, spannableString, 0)
            builder.append(spannableString)
        } else
            builder.append(text).append(" ")

        builder.setSpan(MentionAnnotation.mentionAnnotationForRecipientId(recipientId, text.trim().toString()), 0, builder.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        MentionAnnotation.replaceSpacesWithTransparentLines(builder, 0, builder.length - 1)
        return builder
    }

    private fun findQueryStart(text: CharSequence, inputCursorPosition: Int, keywordEmojiSearch: Boolean): QueryStart? {
        if (keywordEmojiSearch) {
            var start = findQueryStart(text, inputCursorPosition, ' ')
            if (start == -1 && inputCursorPosition != 0) {
                start = 0
            } else if (start == inputCursorPosition)
                start = -1

            return QueryStart(start, false)
        }

        val queryStart = QueryStart(findQueryStart(text, inputCursorPosition, MentionDeleter.MENTION_STARTER), true)

        if (queryStart.index < 0)
            return null

        return QueryStart(findQueryStart(text, inputCursorPosition, MentionDeleter.MENTION_STARTER), true)
    }

    private fun findQueryStart(text: CharSequence, inputCursorPosition: Int, starter: Char): Int {
        if (inputCursorPosition == 0) {
            return -1
        }
        var delimiterSearchIndex = inputCursorPosition - 1
        while (delimiterSearchIndex >= 0 && text[delimiterSearchIndex] != starter && !Character.isWhitespace(text[delimiterSearchIndex])) {
            delimiterSearchIndex--
        }
        val index = if (delimiterSearchIndex >= 0 && text[delimiterSearchIndex] == starter) {
            delimiterSearchIndex + 1
        } else -1

        if (index == -1 || index == 1) return index

        val previousChar = text[index - 2]
        if (previousChar == ' ' || previousChar == '\n')
            return index

        return -1
    }

    private class QueryStart(var index: Int, var isMentionQuery: Boolean)

    interface CursorPositionChangedListener {
        fun onCursorPositionChanged(start: Int, end: Int)
    }
}