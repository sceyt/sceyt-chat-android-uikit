package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultSelfDestructedMessageBodyFormatter : Formatter<SceytMessage> {
    override fun format(context: Context, from: SceytMessage): CharSequence {
        val body = context.getString(R.string.sceyt_message_self_destructed)
        val span = SpannableString(body)
        span.setSpan(
            StyleSpan(Typeface.ITALIC),
            0,
            body.length,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE
        )
        return span
    }
}