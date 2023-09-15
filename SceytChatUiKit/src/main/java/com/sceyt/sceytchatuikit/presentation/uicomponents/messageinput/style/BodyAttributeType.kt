package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style

enum class BodyAttributeType {
    Bold, Italic, Strikethrough, Monospace, Mention;

    override fun toString(): String {
        return when (this) {
            Bold -> "bold"
            Italic -> "italic"
            Strikethrough -> "strikethrough"
            Monospace -> "monospace"
            Mention -> "mention"
        }
    }
}
