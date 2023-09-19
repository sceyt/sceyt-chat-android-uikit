package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style

enum class BodyAttributeType {
    Bold, Italic, Strikethrough, Monospace, Underline, Mention;

    override fun toString(): String {
        return when (this) {
            Bold -> "bold"
            Italic -> "italic"
            Strikethrough -> "strikethrough"
            Monospace -> "monospace"
            Underline -> "underline"
            Mention -> "mention"
        }
    }
}
