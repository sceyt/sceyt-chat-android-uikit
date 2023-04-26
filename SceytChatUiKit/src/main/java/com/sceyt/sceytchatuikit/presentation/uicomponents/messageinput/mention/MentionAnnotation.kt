package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.text.Annotation
import android.text.Spannable
import android.text.Spanned

/**
 * This wraps an Android standard [Annotation] so it can leverage the built in
 * span parceling for copy/paste. The annotation span contains the mentioned recipient's
 * id (in numerical form).
 *
 *
 * Note: Do not extend Annotation or the parceling behavior will be lost.
 */
object MentionAnnotation {
    private const val MENTION_ANNOTATION = "mention"

    fun mentionAnnotationForRecipientId(id: String): Annotation {
        return Annotation(MENTION_ANNOTATION, idToMentionAnnotationValue(id))
    }

    fun idToMentionAnnotationValue(id: String): String {
        return id
    }

    fun isMentionAnnotation(annotation: Annotation): Boolean {
        return MENTION_ANNOTATION == annotation.key
    }

    fun setMentionAnnotations(body: Spannable, mentions: List<Mention>) {
        for ((recipientId, name, start, length) in mentions) {
            body.setSpan(mentionAnnotationForRecipientId(recipientId), start, start + length + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun getMentionsFromAnnotations(text: CharSequence?): List<Mention> {
        if (text is Spanned) {
            return getMentionAnnotations(text).map { annotation ->
                val spanStart = text.getSpanStart(annotation)
                val spanLength = text.getSpanEnd(annotation) - spanStart
                Mention(annotation.value, text.substring(spanStart + 1, spanStart + spanLength), spanStart, spanLength)
            }
        }
        return emptyList()
    }

    fun getMentionAnnotations(spanned: Spanned): List<Annotation> {
        return getMentionAnnotations(spanned, 0, spanned.length)
    }

    @JvmStatic
    fun getMentionAnnotations(spanned: Spanned, start: Int, end: Int): List<Annotation> {
        return spanned.getSpans(start, end, Annotation::class.java)
            .filter(MentionAnnotation::isMentionAnnotation)
    }
}