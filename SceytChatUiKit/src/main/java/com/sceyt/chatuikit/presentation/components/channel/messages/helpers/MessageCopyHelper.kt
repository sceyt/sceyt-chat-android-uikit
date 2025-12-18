package com.sceyt.chatuikit.presentation.components.channel.messages.helpers

import android.content.Context
import android.text.SpannableStringBuilder
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyAttributeType
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionAnnotation
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionUserHelper
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper

/**
 * Helper class to convert messages with body attributes (mentions and styling)
 * to spannable text that can be copied and pasted into the input field.
 */
object MessageCopyHelper {

    /**
     * Converts multiple messages to a single copyable text with formatting preserved.
     *
     * @param messages The messages to convert
     * @param context The context for formatting
     * @return SpannableStringBuilder with all messages separated by double newlines
     */
    fun buildCopyableText(context: Context, vararg messages: SceytMessage): CharSequence {
        if (messages.isEmpty()) return ""
        if (messages.size == 1)
            return buildCopyableTextImpl(context = context, message = messages[0])

        val builder = SpannableStringBuilder()
        messages.forEachIndexed { index, message ->
            val copyableText = buildCopyableTextImpl(context = context, message = message)
            builder.append(copyableText)

            // Add separator between messages (but not after the last one)
            if (index < messages.size - 1) {
                builder.append("\n\n")
            }
        }

        return builder
    }

    /**
     * Converts a message body with body attributes to a SpannableStringBuilder
     * that contains both styling spans and mention annotations.
     *
     * This method:
     * 1. Applies text styling (bold, italic, etc.) using MessageBodyStyleHelper
     * 2. Replaces @userId with @DisplayName and adds Annotation spans using MentionAnnotation
     *
     * @param context The context for formatting
     * @param message The message to convert
     * @return SpannableStringBuilder with styling and mention annotations
     */
    private fun buildCopyableTextImpl(context: Context, message: SceytMessage): CharSequence {
        val body = message.body
        if (body.isBlank()) return body

        val bodyAttributes = message.bodyAttributes
        if (bodyAttributes.isNullOrEmpty()) return body

        // Step 1: Apply text styling (bold, italic, strikethrough, etc.)
        val styledBody = MessageBodyStyleHelper.buildOnlyTextStyles(body, bodyAttributes)

        // Step 2: Get mentions from body attributes
        val mentionAttributes = bodyAttributes.filter {
            it.type == BodyAttributeType.Mention.value
        }

        if (mentionAttributes.isEmpty()) {
            return styledBody
        }

        // Step 3: Convert body attributes to Mention objects with display names
        val mentions = MentionUserHelper.getMentionsIndexed(
            context = context,
            attributes = mentionAttributes,
            mentionUsers = message.mentionedUsers
        )

        // Step 4: Use MentionAnnotation.setMentionAnnotations to replace @userId with @DisplayName
        // and add Annotation spans. This method handles text replacement and annotation creation.
        val bodyWithMentions = MentionAnnotation.setMentionAnnotations(styledBody, mentions)

        return SpannableStringBuilder(bodyWithMentions)
    }
}

