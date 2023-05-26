package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.graphics.Color
import android.text.Annotation
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import com.google.gson.Gson
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper.getValueData

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

    fun mentionAnnotationForRecipientId(id: String, name: String): Annotation {
        return Annotation(MENTION_ANNOTATION, createAnnotationValue(id, name))
    }

    fun createAnnotationValue(id: String, name: String): String {
        return Gson().toJson(MentionAnnotationValue(name, id))
    }

    fun isMentionAnnotation(annotation: Annotation): Boolean {
        return MENTION_ANNOTATION == annotation.key
    }

    fun setMentionAnnotations(body: Spannable, mentions: List<Mention>): SpannableString {
        val newBody = SpannableStringBuilder(body)
        for ((recipientId, name, start, length) in mentions.sortedByDescending { it.start }) {
            try {
                newBody.replace(start, start + length, "@$name")
                newBody.setSpan(mentionAnnotationForRecipientId(recipientId, name), start, start + name.length + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                replaceSpacesWithTransparentLines(newBody, start, start + name.length)
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't set mention annotation for recipient id: $recipientId")
            }
        }
        return SpannableString(newBody)
    }

    fun getMentionsFromAnnotations(text: CharSequence?): List<Mention> {
        if (text is Spanned) {
            return getMentionAnnotations(text).map { annotation ->
                val spanStart = text.getSpanStart(annotation)
                val spanLength = text.getSpanEnd(annotation) - spanStart
                val valueData = annotation.getValueData() ?: return emptyList()
                Mention(valueData.userId, valueData.userName, spanStart, spanLength)
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

    fun replaceSpacesWithTransparentLines(text: SpannableStringBuilder, start: Int, end: Int) {
        (start..end).forEach {
            if (it != text.length - 1 && it in text.indices && text[it] == ' ') {
                text.replace(it, it + 1, "-")
                text.setSpan(ForegroundColorSpan(Color.TRANSPARENT), it, it + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
