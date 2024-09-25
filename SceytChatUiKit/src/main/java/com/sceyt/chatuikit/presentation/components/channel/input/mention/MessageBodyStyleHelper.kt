package com.sceyt.chatuikit.presentation.components.channel.input.mention

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import androidx.annotation.ColorInt
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyAttributeType
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyler
import com.sceyt.chatuikit.presentation.components.channel.input.format.StyleType
import com.sceyt.chatuikit.presentation.components.channel.input.format.toStyleType
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_STYLE

object MessageBodyStyleHelper {

    fun buildOnlyStylesWithAttributes(
            body: CharSequence,
            attribute: List<BodyAttribute>?
    ): CharSequence {
        if (attribute.isNullOrEmpty()) return body
        return appendTextStyle(body, attribute)
    }

    fun buildWithAllAttributes(
            message: SceytMessage,
            @ColorInt color: Int = 0,
            style: Int = UNSET_STYLE,
    ): CharSequence {
        return buildWithAllAttributes(
            body = message.body,
            mentionUsers = message.mentionedUsers,
            bodyAttributes = message.bodyAttributes,
            color = color,
            style = style)
    }

    fun buildWithAllAttributes(
            body: CharSequence,
            mentionUsers: List<User>?,
            bodyAttributes: List<BodyAttribute>?,
            @ColorInt color: Int = 0,
            style: Int = UNSET_STYLE,
    ): CharSequence {
        return appendBodyAttributes(
            body = body,
            list = bodyAttributes,
            mentionUsers = mentionUsers,
            color = color,
            style = style)
    }

    fun buildOnlyBoldMentionsAndStylesWithAttributes(message: SceytMessage): CharSequence {
        return buildOnlyBoldMentionsAndStylesWithAttributes(message.body, message.mentionedUsers, message.bodyAttributes)
    }

    fun buildOnlyBoldMentionsAndStylesWithAttributes(body: CharSequence, mentionUsers: List<User>?, bodyAttributes: List<BodyAttribute>?): CharSequence {
        return appendStyleOnlyWithBoldMentions(body, bodyAttributes, mentionUsers)
    }

    private fun appendBodyAttributes(
            body: CharSequence,
            list: List<BodyAttribute>?,
            mentionUsers: List<User>?,
            @ColorInt color: Int = 0,
            style: Int = UNSET_STYLE,
    ): CharSequence {
        list ?: return body
        val group = list.groupBy { it.type == BodyAttributeType.Mention.toString() }
        var spannableString = appendTextStyle(body, group[false])

        group[true]?.let {
            spannableString = MentionUserHelper.buildWithMentionedUsers(
                body = spannableString,
                mentionAttributes = it,
                mentionUsers = mentionUsers,
                color = color,
                style = style)
        }

        return spannableString
    }

    private fun appendStyleOnlyWithBoldMentions(
            body: CharSequence,
            list: List<BodyAttribute>?,
            mentionUsers: List<User>?,
            style: Int = Typeface.BOLD
    ): CharSequence {
        list ?: return body
        val group = list.groupBy { it.type == BodyAttributeType.Mention.toString() }
        var spannableString = appendTextStyle(body, group[false])

        group[true]?.let {
            spannableString = MentionUserHelper.buildOnlyBoldNamesWithMentionedUsers(
                spannableBody = spannableString,
                attributes = it,
                mentionUsers = mentionUsers,
                style = style)
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