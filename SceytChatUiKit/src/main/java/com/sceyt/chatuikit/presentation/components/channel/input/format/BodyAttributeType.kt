package com.sceyt.chatuikit.presentation.components.channel.input.format

import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionUserHelper

enum class BodyAttributeType(val value: String) {
    Bold("bold"),
    Italic("italic"),
    Strikethrough("strikethrough"),
    Monospace("monospace"),
    Underline("underline"),
    Mention(MentionUserHelper.MENTION);
}
