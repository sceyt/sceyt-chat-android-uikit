package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style

enum class StyleType {
    Bold, Italic, Strikethrough, Monospace, Underline;

    override fun toString(): String {
        return when (this) {
            Bold -> "bold"
            Italic -> "italic"
            Strikethrough -> "strikethrough"
            Monospace -> "monospace"
            Underline -> "underline"
        }
    }
}

fun String.toStyleType(): StyleType? {
    return when (this) {
        "bold" -> StyleType.Bold
        "italic" -> StyleType.Italic
        "strikethrough" -> StyleType.Strikethrough
        "monospace" -> StyleType.Monospace
        "underline" -> StyleType.Underline
        else -> null
    }
}