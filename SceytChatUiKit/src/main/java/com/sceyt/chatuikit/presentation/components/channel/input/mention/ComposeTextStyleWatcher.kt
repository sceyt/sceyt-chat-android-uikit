package com.sceyt.chatuikit.presentation.components.channel.input.mention

import android.content.Context
import android.text.Annotation
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import androidx.core.text.getSpans
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isVisuallyEmpty
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyler
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyler.isSupportedStyle
import com.sceyt.chatuikit.styles.common.TextStyle


class ComposeTextStyleWatcher(private val context: Context) : TextWatcher {
    private val markerAnnotation = Annotation("text-formatting", "marker")
    private var textSnapshotPriorToChange: CharSequence? = null
    private var mentionTextStyle = TextStyle(color = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (s is Spannable)
            s.removeSpan(markerAnnotation)

        textSnapshotPriorToChange = s.subSequence(start, start + count)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s is Spannable) {
            s.removeSpan(markerAnnotation)

            if (count > 0) {
                s.setSpan(markerAnnotation, start, start + count, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        // Set mention spans
        s.getSpans<Annotation>(0, s.length).forEach {
            if (it.key == MentionUserHelper.MENTION) {
                mentionTextStyle.apply(context, s, s.getSpanStart(it), s.getSpanEnd(it))
            }
        }

        // Set text formatting spans
        val editStart = s.getSpanStart(markerAnnotation)
        val editEnd = s.getSpanEnd(markerAnnotation)
        s.removeSpan(markerAnnotation)

        try {
            if (editStart < 0 || editEnd < 0 || editStart >= editEnd || (editStart == 0 && editEnd == s.length)) {
                textSnapshotPriorToChange = null
                return
            }

            val change = s.subSequence(editStart, editEnd)
            if (change.isEmpty() ||
                    textSnapshotPriorToChange == null ||
                    (editEnd - editStart == 1 && !change[0].isVisuallyEmpty()) ||
                    TextUtils.equals(textSnapshotPriorToChange, change) ||
                    editEnd - editStart > 1
            ) {
                textSnapshotPriorToChange = null
                return
            }
            textSnapshotPriorToChange = null

            var newEnd = editStart
            for (i in change.indices) {
                if (change[i].isVisuallyEmpty()) {
                    newEnd = editStart + i
                    break
                }
            }

            s.getSpans(editStart, editEnd, Object::class.java)
                .filter { it.isSupportedStyle() }
                .forEach { style ->
                    val styleStart = s.getSpanStart(style)
                    val styleEnd = s.getSpanEnd(style)
                    if (styleEnd == editEnd && styleStart < styleEnd) {
                        s.removeSpan(style)
                        s.setSpan(style, styleStart, newEnd, BodyStyler.SPAN_FLAGS)
                    } else if (styleStart >= styleEnd) {
                        s.removeSpan(style)
                    }
                }
        } finally {
            s.getSpans(editStart, editEnd, Object::class.java)
                .filter { it.isSupportedStyle() }
                .forEach { style ->
                    val styleStart = s.getSpanStart(style)
                    val styleEnd = s.getSpanEnd(style)
                    if (styleEnd == styleStart || styleStart > styleEnd) {
                        s.removeSpan(style)
                    }
                }
        }
    }

    fun setMentionTextStyle(style: TextStyle) {
        mentionTextStyle = style
    }
}
