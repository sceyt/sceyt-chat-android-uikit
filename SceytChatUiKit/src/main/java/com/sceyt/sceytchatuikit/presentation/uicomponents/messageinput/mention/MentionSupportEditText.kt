package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.graphics.Typeface
import android.text.Annotation
import android.text.Editable
import android.text.Selection
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionValidatorWatcher.MentionValidator
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQuery
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQueryChangedListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQueryReplacement
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyler
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.StyleType


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
    private var stylingChangedListener: StylingChangedListener? = null

    private fun initialize() {
        addTextChangedListener(mentionValidatorWatcher)
        addTextChangedListener(MentionDeleter())
        addTextChangedListener(ComposeTextStyleWatcher(context))

        doAfterTextChanged {
            onInputTextChanged(it ?: return@doAfterTextChanged)
        }

        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val copy = menu.findItem(android.R.id.copy)
                val cut = menu.findItem(android.R.id.cut)
                val paste = menu.findItem(android.R.id.paste)
                val copyOrder = copy?.order ?: 0
                val cutOrder = cut?.order ?: 0
                val pasteOrder = paste?.order ?: 0
                val largestOrder = maxOf(copyOrder, cutOrder, pasteOrder)
                menu.add(0, R.id.sceyt_bold, largestOrder, SpannableString(context.getString(R.string.sceyt_bold)).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                })
                menu.add(0, R.id.sceyt_italic, largestOrder, SpannableString(context.getString(R.string.sceyt_italic)).apply {
                    setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                })
                menu.add(0, R.id.sceyt_strikethrough, largestOrder, SpannableString(context.getString(R.string.sceyt_strikethrough)).apply {
                    setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                })
                menu.add(0, R.id.sceyt_monospace, largestOrder, SpannableString(context.getString(R.string.sceyt_monospace)).apply {
                    setSpan(TypefaceSpan(BodyStyler.MONOSPACE), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                })
                text?.let {
                    val start = selectionStart
                    val end = selectionEnd
                    if (BodyStyler.hasStyling(it, start, end))
                        menu.add(0, R.id.sceyt_clear_formatting, largestOrder, context.getString(R.string.sceyt_clear_formatting))
                }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val handled: Boolean = handleFormatText(item.itemId)
                if (handled) {
                    mode.finish()
                }
                return handled
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
        }
    }

    fun handleFormatText(@IdRes id: Int): Boolean {
        val text = text ?: return false
        if (id != R.id.sceyt_bold && id != R.id.sceyt_italic && id != R.id.sceyt_strikethrough
                && id != R.id.sceyt_monospace  && id != R.id.sceyt_clear_formatting) {
            return false
        }
        val start = selectionStart
        val end = selectionEnd
        var style: StyleType? = null
        when (id) {
            R.id.sceyt_bold -> style = StyleType.Bold
            R.id.sceyt_italic -> style = StyleType.Italic
            R.id.sceyt_strikethrough -> style = StyleType.Strikethrough
            R.id.sceyt_monospace -> style = StyleType.Monospace
        }
        clearComposingText()
        if (style != null) {
            BodyStyler.toggleStyle(style, text, start, end)
        } else {
            BodyStyler.clearStyling(text, start, end)
        }
        Selection.setSelection(getText(), end)
        stylingChangedListener?.onStylingChanged()
        return true
    }

    override fun onSelectionChanged(selectionStart: Int, selectionEnd: Int) {
        super.onSelectionChanged(selectionStart, selectionEnd)
        text?.let { inputText ->
            val selectionChanged = changeSelectionForPartialMentions(inputText, selectionStart, selectionEnd)
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

    fun setStylingChangedListener(listener: StylingChangedListener?) {
        stylingChangedListener = listener
    }

    fun hasMentions(): Boolean {
        val text = text
        return if (text != null) {
            MentionAnnotation.getMentionAnnotations(text).isNotEmpty()
        } else false
    }

    val mentions: List<Mention>
        get() = MentionAnnotation.getMentionsFromAnnotations(text)

    fun hasStyling(): Boolean {
        val trimmed: CharSequence = text?.trim() ?: return false
        return trimmed is Spanned && BodyStyler.hasStyling(trimmed)
    }

    val styling: List<BodyStyleRange>?
        get() =  BodyStyler.getStyling(text)

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
            Log.e("ComposeText", "Failed to replace text with mention.")
        }
    }

    private fun createReplacementToken(text: CharSequence, recipientId: String): CharSequence {
        val builder = SpannableStringBuilder().append(MentionDeleter.MENTION_STARTER)
        if (text is Spanned) {
            val spannableString = SpannableString("$text ")
            TextUtils.copySpansFrom(text, 0, text.length, Any::class.java, spannableString, 0)
            builder.append(spannableString)
        } else builder.append(text).append(" ")
        builder.setSpan(MentionAnnotation.mentionAnnotationForRecipientId(recipientId, text.trim().toString()), 0, builder.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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

    fun interface CursorPositionChangedListener {
        fun onCursorPositionChanged(start: Int, end: Int)
    }

    fun interface StylingChangedListener {
        fun onStylingChanged()
    }
}