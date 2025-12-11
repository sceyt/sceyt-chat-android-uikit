package com.sceyt.chatuikit.presentation.components.channel.messages.helpers

import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LineHeightSpan
import android.view.View
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.messages_list.item.ReadMoreStyle

/**
 * Helper object for truncating message body text and adding "Read More" functionality.
 */
object MessageBodyTruncationHelper {

    /**
     * Custom LineHeightSpan to add extra spacing above text.
     */
    private class SpacingSpan(private val extraSpacingPx: Int) : LineHeightSpan {
        override fun chooseHeight(
            text: CharSequence?,
            start: Int,
            end: Int,
            spanstartv: Int,
            lineHeight: Int,
            fm: Paint.FontMetricsInt?
        ) {
            fm?.let {
                it.top -= extraSpacingPx
                it.ascent -= extraSpacingPx
            }
        }
    }

    /**
     * Applies truncation to the message body if it exceeds the character limit.
     * If the message is already expanded or within the limit, returns the original formatted body.
     *
     * @param formattedBody The fully formatted message body with mentions, links, etc.
     * @param isExpanded Whether the message is currently expanded
     * @param characterLimit The maximum number of characters to show when collapsed
     * @param readMoreStyle The style configuration for the "Read More" text
     * @param onReadMoreClick Callback to invoke when "Read More" is clicked
     * @return The truncated CharSequence with "Read More" appended, or the original if no truncation needed
     */
    fun applyTruncationIfNeeded(
        formattedBody: CharSequence,
        isExpanded: Boolean,
        characterLimit: Int,
        readMoreStyle: ReadMoreStyle,
        onReadMoreClick: () -> Unit
    ): CharSequence {
        if (isExpanded || characterLimit == Integer.MAX_VALUE || formattedBody.length <= characterLimit) {
            return formattedBody
        }

        val spannable = SpannableStringBuilder()

        spannable.append(formattedBody.take(characterLimit))
        spannable.append("...")
        spannable.append("\n")

        val readMoreStart = spannable.length
        spannable.append(readMoreStyle.text)
        val readMoreEnd = spannable.length
        
        val spacingPx = 8.dpToPx()
        spannable.setSpan(
            SpacingSpan(spacingPx),
            readMoreStart,
            readMoreEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (readMoreStyle.textStyle.color != UNSET_COLOR) {
            spannable.setSpan(
                ForegroundColorSpan(readMoreStyle.textStyle.color),
                readMoreStart,
                readMoreEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                onReadMoreClick()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                if (readMoreStyle.textStyle.color != UNSET_COLOR) {
                    ds.color = readMoreStyle.textStyle.color
                }
            }
        }

        spannable.setSpan(
            clickableSpan,
            readMoreStart,
            readMoreEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }
}