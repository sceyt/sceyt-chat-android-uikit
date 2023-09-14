package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style

enum class StyleType {
    Bold, Italic, Strikethrough, Monospace;

    override fun toString(): String {
        return when (this) {
            Bold -> "bold"
            Italic -> "italic"
            Strikethrough -> "strikethrough"
            Monospace -> "monospace"
        }
    }
}

fun String.toStyleType(): StyleType? {
    return when (this) {
        "bold" -> StyleType.Bold
        "italic" -> StyleType.Italic
        "strikethrough" -> StyleType.Strikethrough
        "monospace" -> StyleType.Monospace
        else -> null
    }
}