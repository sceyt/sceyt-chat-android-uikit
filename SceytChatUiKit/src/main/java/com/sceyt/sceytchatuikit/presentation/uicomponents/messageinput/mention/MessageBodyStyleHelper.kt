package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.text.SpannableStringBuilder
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyAttributeType
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyler
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.StyleType
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.toStyleType

object MessageBodyStyleHelper {

    fun buildOnlyStylesWithAttributes(body: CharSequence, attribute: List<BodyAttribute>?): CharSequence {
        if (attribute.isNullOrEmpty()) return body
        return appStyle(body, attribute)
    }

    fun buildWithMentionsAndAttributes(context: Context, message: SceytMessage): CharSequence {
        return buildWithMentionsAndAttributes(context, message.body, message.mentionedUsers, message.bodyAttributes)
    }

    fun buildWithMentionsAndAttributes(context: Context, body: String, mentionUsers: Array<User>?, bodyAttributes: List<BodyAttribute>?): CharSequence {
        return appendStyle(context, body, bodyAttributes, mentionUsers)
    }

    fun buildOnlyBoldMentionsAndStylesWithAttributes(message: SceytMessage): CharSequence {
        return buildOnlyBoldMentionsAndStylesWithAttributes(message.body, message.mentionedUsers, message.bodyAttributes)
    }

    fun buildOnlyBoldMentionsAndStylesWithAttributes(body: String, mentionUsers: Array<User>?, bodyAttributes: List<BodyAttribute>?): CharSequence {
        return appendStyleOnlyWithBoldMentions(body, bodyAttributes, mentionUsers)
    }


    private fun appendStyle(context: Context, body: String, list: List<BodyAttribute>?, mentionUsers: Array<User>?): CharSequence {
        list ?: return body
        val group = list.groupBy { it.type == BodyAttributeType.Mention.toString() }
        var spannableString = appStyle(body, group[false])

        group[true]?.let {
            spannableString = MentionUserHelper.buildWithMentionedUsers(context, spannableString, it, mentionUsers)
        }

        return spannableString
    }

    private fun appendStyleOnlyWithBoldMentions(body: String, list: List<BodyAttribute>?, mentionUsers: Array<User>?): CharSequence {
        list ?: return body
        val group = list.groupBy { it.type == BodyAttributeType.Mention.toString() }
        var spannableString = appStyle(body, group[false])

        group[true]?.let {
            spannableString = MentionUserHelper.buildOnlyBoldNamesWithMentionedUsers(spannableString, it, mentionUsers)
        }

        return spannableString
    }

    private fun appStyle(body: CharSequence, list: List<BodyAttribute>?): CharSequence {
        val spannableString = SpannableStringBuilder(body)
        list ?: return spannableString
        try {
            list.forEach {
                when (it.type.toStyleType()) {
                    StyleType.Bold -> spannableString.setSpan(BodyStyler.boldStyle(), it.offset, it.offset + it.length, BodyStyler.SPAN_FLAGS)
                    StyleType.Italic -> spannableString.setSpan(BodyStyler.italicStyle(), it.offset, it.offset + it.length, BodyStyler.SPAN_FLAGS)
                    StyleType.Strikethrough -> spannableString.setSpan(BodyStyler.strikethroughStyle(), it.offset, it.offset + it.length, BodyStyler.SPAN_FLAGS)
                    StyleType.Monospace -> {
                        val end = it.offset + it.length
                        spannableString.setSpan(BodyStyler.monoStyle(), it.offset, end, BodyStyler.SPAN_FLAGS)
                        if (end == body.length)
                            spannableString.append("\u202F")
                    }

                    StyleType.Underline -> spannableString.setSpan(BodyStyler.underlineStyle(), it.offset, it.offset + it.length, BodyStyler.SPAN_FLAGS)
                    null -> return@forEach
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return spannableString
    }
}