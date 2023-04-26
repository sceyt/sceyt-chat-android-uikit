package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.text.Annotation
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionAnnotation.getMentionAnnotations

/**
 * Detects if some part of the mention is being deleted, and if so, deletes the entire mention and
 * span from the text view.
 */
class MentionDeleter : TextWatcher {
    private var toDelete: Annotation? = null

    override fun beforeTextChanged(sequence: CharSequence, start: Int, count: Int, after: Int) {
        if (count > 0 && sequence is Spanned) {
            for (annotation in getMentionAnnotations(sequence, start, start + count)) {
                if (sequence.getSpanStart(annotation) < start && sequence.getSpanEnd(annotation) > start) {
                    toDelete = annotation
                    return
                }
            }
        }
    }

    override fun afterTextChanged(editable: Editable) {
        if (toDelete == null) {
            return
        }
        val toDeleteStart = editable.getSpanStart(toDelete)
        val toDeleteEnd = editable.getSpanEnd(toDelete)
        editable.removeSpan(toDelete)
        toDelete = null
        editable.replace(toDeleteStart, toDeleteEnd, "")
    }

    override fun onTextChanged(sequence: CharSequence, start: Int, before: Int, count: Int) {}

    companion object {
        const val MENTION_STARTER = '@'
    }
}