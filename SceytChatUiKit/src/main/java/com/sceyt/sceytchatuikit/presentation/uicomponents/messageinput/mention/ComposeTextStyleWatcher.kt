package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.graphics.Color
import android.text.Annotation
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig


class ComposeTextStyleWatcher(private val context: Context) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable) {
        val annotation = s.getSpans<Annotation>(0, s.length)
        if (annotation.isEmpty()) return

        val existingSpans = s.getSpans<ForegroundColorSpan>(0, s.length).filter { it.foregroundColor == Color.GREEN }

        annotation.forEach {
            if (it.key == "mention")
                s.setSpan(ForegroundColorSpan(context.getCompatColor(SceytKitConfig.sceytColorAccent)), s.getSpanStart(it), s.getSpanEnd(it), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        for (span in existingSpans) {
            val spanStart: Int = s.getSpanStart(span)
            val spanEnd: Int = s.getSpanEnd(span)
            val spanFlags: Int = s.getSpanFlags(span)
            s.removeSpan(span)
            s.setSpan(span, spanStart, spanEnd, spanFlags)
        }
    }
}
