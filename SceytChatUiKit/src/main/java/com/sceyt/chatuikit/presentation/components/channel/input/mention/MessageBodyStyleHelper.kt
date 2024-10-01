package com.sceyt.chatuikit.presentation.components.channel.input.mention

import android.content.Context
import android.text.SpannableStringBuilder
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyAttributeType
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyler
import com.sceyt.chatuikit.presentation.components.channel.input.format.StyleType
import com.sceyt.chatuikit.presentation.components.channel.input.format.toStyleType
import com.sceyt.chatuikit.styles.common.TextStyle

object MessageBodyStyleHelper {

    fun buildOnlyTextStyles(
            body: CharSequence,
            attribute: List<BodyAttribute>?
    ): CharSequence {
        if (attribute.isNullOrEmpty()) return body
        return appendTextStyle(body, attribute)
    }

    fun SceytMessage.buildWithAttributes(
            context: Context,
            mentionTextStyle: TextStyle,
            mentionUserNameFormatter: Formatter<SceytUser>
    ): CharSequence {
        return buildWithAttributes(
            context = context,
            body = body,
            mentionUsers = mentionedUsers,
            bodyAttributes = bodyAttributes,
            mentionTextStyle = mentionTextStyle,
            mentionUserNameFormatter = mentionUserNameFormatter
        )
    }

    fun buildWithAttributes(
            context: Context,
            body: CharSequence,
            mentionUsers: List<SceytUser>?,
            bodyAttributes: List<BodyAttribute>?,
            mentionTextStyle: TextStyle,
            mentionUserNameFormatter: Formatter<SceytUser>
    ): CharSequence {
        return appendAttributes(
            context = context,
            body = body,
            list = bodyAttributes,
            mentionUsers = mentionUsers,
            mentionTextStyle = mentionTextStyle,
            mentionUserNameFormatter = mentionUserNameFormatter
        )
    }

    private fun appendAttributes(
            context: Context,
            body: CharSequence,
            list: List<BodyAttribute>?,
            mentionUsers: List<SceytUser>?,
            mentionTextStyle: TextStyle,
            mentionUserNameFormatter: Formatter<SceytUser>
    ): CharSequence {
        list ?: return body
        val group = list.groupBy { it.type == BodyAttributeType.Mention.value }
        var spannableString = appendTextStyle(body, group[false])

        group[true]?.let {
            spannableString = MentionUserHelper.buildWithMentionedUsers(
                context = context,
                body = spannableString,
                mentionAttributes = it,
                mentionUsers = mentionUsers,
                mentionTextStyle = mentionTextStyle,
                mentionUserNameFormatter = mentionUserNameFormatter)
        }

        return spannableString
    }

    private fun appendTextStyle(body: CharSequence, list: List<BodyAttribute>?): CharSequence {
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