package com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention

import android.text.Annotation
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.Log
import com.google.gson.Gson
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.notAutoCorrectable
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper.getValueData

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

    fun setMentionAnnotations(body: CharSequence, mentions: List<Mention>): SpannableString {
        val newBody = SpannableStringBuilder(body)
        for ((recipientId, name, start, length) in mentions.sortedByDescending { it.start }) {
            try {
                val newName = "@$name".notAutoCorrectable()
                newBody.replace(start, start + length, newName)
                newBody.setSpan(mentionAnnotationForRecipientId(recipientId, name), start, start + newName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
}
