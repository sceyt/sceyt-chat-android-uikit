package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
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
        s.getSpans<Annotation>(0, s.length).forEach {
            if (it.key == "mention")
                s.setSpan(ForegroundColorSpan(context.getCompatColor(SceytKitConfig.sceytColorAccent)), s.getSpanStart(it), s.getSpanEnd(it), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
