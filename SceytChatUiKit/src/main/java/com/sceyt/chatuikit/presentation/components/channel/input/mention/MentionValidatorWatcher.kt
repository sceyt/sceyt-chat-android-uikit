package com.sceyt.chatuikit.presentation.components.channel.input.mention

import android.text.Annotation
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionAnnotation.getMentionAnnotations

/**
 * Provides a mechanism to validate mention annotations set on an edit text. This enables
 * removing invalid mentions if the user mentioned isn't in the group.
 */
class MentionValidatorWatcher : TextWatcher {
    private var invalidMentionAnnotations: List<Annotation>? = null
    private var mentionValidator: MentionValidator? = null

    override fun onTextChanged(sequence: CharSequence, start: Int, before: Int, count: Int) {
        if (count > 1 && mentionValidator != null && sequence is Spanned) {
            val mentionAnnotations: List<Annotation> = getMentionAnnotations(sequence, start, start + count)
            if (mentionAnnotations.isNotEmpty())
                invalidMentionAnnotations = mentionValidator?.getInvalidMentionAnnotations(mentionAnnotations)
        }
    }

    override fun afterTextChanged(editable: Editable) {
        val invalidMentions: List<Annotation> = invalidMentionAnnotations ?: return
        invalidMentionAnnotations = null
        for (annotation in invalidMentions) {
            val spanStart = editable.getSpanStart(annotation)
            val spanEnd = editable.getSpanEnd(annotation)
            editable.getSpans<ForegroundColorSpan>(spanStart, spanEnd).forEach {
                editable.removeSpan(it)
            }
            editable.removeSpan(annotation)
            (spanStart until spanEnd).forEach {
                if (editable[it] == '-')
                    editable.replace(it, it + 1, " ")
            }
        }
    }

    fun setMentionValidator(mentionValidator: MentionValidator?) {
        this.mentionValidator = mentionValidator
    }

    override fun beforeTextChanged(sequence: CharSequence, start: Int, count: Int, after: Int) {}

    interface MentionValidator {
        fun getInvalidMentionAnnotations(mentionAnnotations: List<Annotation>?): List<Annotation>?
    }
}