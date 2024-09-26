package com.sceyt.chatuikit.presentation.components.channel.input.format

import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionUserHelper

enum class BodyAttributeType {
    Bold, Italic, Strikethrough, Monospace, Underline, Mention;

    fun value(): String {
        return when (this) {
            Bold -> "bold"
            Italic -> "italic"
            Strikethrough -> "strikethrough"
            Monospace -> "monospace"
            Underline -> "underline"
            Mention -> MentionUserHelper.MENTION
        }
    }
}
